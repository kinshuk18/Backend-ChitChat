package com.example.chitchat.repository;

import com.example.chitchat.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity,String>{}
