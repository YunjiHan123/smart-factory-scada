package com.smartfactory.scada.user.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.smartfactory.scada.user.domain.User;

@Mapper
public interface UserMapper {

	Optional<User> findById(@Param("id") Long id);

	Optional<User> findByEmail(@Param("email") String email);

	boolean existsByEmail(@Param("email") String email);

	void insert(User user);

	void updateLastLoginAt(@Param("id") Long id);
}
