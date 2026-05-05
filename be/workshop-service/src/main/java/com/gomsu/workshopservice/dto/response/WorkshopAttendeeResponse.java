package com.gomsu.workshopservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkshopAttendeeResponse {
    private Long workshopId;
    private String workshopName;
    private Integer maxParticipants;      // Số lượng tối đa của workshop
    private Integer currentParticipants;  // Tổng số người đã đăng ký thành công (Sum of tickets)
    private Page<RegistrationResponse> attendees; // Danh sách chi tiết
}
