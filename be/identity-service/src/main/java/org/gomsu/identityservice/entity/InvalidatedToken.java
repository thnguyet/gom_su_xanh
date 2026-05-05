package org.gomsu.identityservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvalidatedToken {
    @Id
    private String id; // Đây chính là JTI (JWT ID)
    private Date expiryTime; // Thời gian hết hạn của Token đó
}