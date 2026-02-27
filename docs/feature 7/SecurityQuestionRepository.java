package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.user.SecurityQuestion;
import com.revature.passwordmanager.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityQuestionRepository extends JpaRepository<SecurityQuestion, Long> {
  List<SecurityQuestion> findAllByUser(User user);

  void deleteAllByUser(User user);
}
