package com.smartfactory.scada.user.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.smartfactory.scada.user.domain.User;
import com.smartfactory.scada.user.dto.UserListRequest;

@Mapper
public interface UserMapper {

	Optional<User> findById(@Param("id") Long id);

	Optional<User> findByEmail(@Param("email") String email);

	boolean existsByEmail(@Param("email") String email);

	void insert(User user);

	void updateLastLoginAt(@Param("id") Long id);

	List<User> findUsers(@Param("request") UserListRequest request);

	long countUsers(@Param("request") UserListRequest request);
}
