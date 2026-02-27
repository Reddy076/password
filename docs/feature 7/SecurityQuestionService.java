package com.revature.passwordmanager.service.auth;

import com.revature.passwordmanager.dto.SecurityQuestionDTO;
import com.revature.passwordmanager.model.user.SecurityQuestion;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.SecurityQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityQuestionService {

  private final SecurityQuestionRepository securityQuestionRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public void saveSecurityQuestions(User user, List<SecurityQuestionDTO> questions) {
    if (questions == null || questions.isEmpty()) {
      return;
    }

    // Clear existing questions if any (for updates)
    securityQuestionRepository.deleteAllByUser(user);

    List<SecurityQuestion> securityQuestions = questions.stream()
        .map(dto -> SecurityQuestion.builder()
            .user(user)
            .questionText(dto.getQuestion())
            .answerHash(hashAnswer(dto.getAnswer()))
            .build())
        .collect(Collectors.toList());

    securityQuestionRepository.saveAll(securityQuestions);
  }

  @Transactional(readOnly = true)
  public boolean verifySecurityAnswers(User user, List<SecurityQuestionDTO> providedAnswers) {
    List<SecurityQuestion> storedQuestions = securityQuestionRepository.findAllByUser(user);

    if (storedQuestions.isEmpty() || providedAnswers == null || providedAnswers.size() != storedQuestions.size()) {
      return false;
    }

    // We assumed the order matches or we need to match by question text.
    // Ideally, the DTO should probably include the question ID for verification,
    // but the requirement "verify identity during password reset" usually implies
    // retrieving the questions first, then sending answers.
    // For this implementation, let's match by Question Text.

    for (SecurityQuestionDTO dto : providedAnswers) {
      boolean matchFound = storedQuestions.stream()
          .filter(sq -> sq.getQuestionText().equals(dto.getQuestion()))
          .anyMatch(sq -> passwordEncoder.matches(normalize(dto.getAnswer()), sq.getAnswerHash()));

      if (!matchFound) {
        return false;
      }
    }

    return true;
  }

  public List<SecurityQuestion> getSecurityQuestions(User user) {
    return securityQuestionRepository.findAllByUser(user);
  }

  private String hashAnswer(String answer) {
    return passwordEncoder.encode(normalize(answer));
  }

  private String normalize(String input) {
    return input == null ? "" : input.trim().toLowerCase();
  }
}
