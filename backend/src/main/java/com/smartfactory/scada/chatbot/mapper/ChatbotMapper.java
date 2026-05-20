package com.smartfactory.scada.chatbot.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.smartfactory.scada.chatbot.domain.ChatbotMessage;

@Mapper
public interface ChatbotMapper {

	void insert(ChatbotMessage message);

	Optional<ChatbotMessage> findById(@Param("id") Long id);

	List<ChatbotMessage> findRecent(
		@Param("userId") Long userId,
		@Param("plantId") Long plantId,
		@Param("limit") int limit
	);

	int deleteByIdAndUserId(
		@Param("id") Long id,
		@Param("userId") Long userId
	);

	int deleteByUserIdAndPlantId(
		@Param("userId") Long userId,
		@Param("plantId") Long plantId
	);
}
