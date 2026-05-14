package com.hamcheese.roomincode.domain.room.dto;

import com.hamcheese.roomincode.domain.room.type.RoomEventType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomMessageDto {
    private RoomEventType type;
    private String roomId;
    private String senderId;
    private Object data;    //안내메시지 or redirect url
}
