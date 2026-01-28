package com.travel.planner.domain.user.repository;

import com.travel.planner.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    java.util.Optional<User> findByEmail(String email);
}