package com.onseju.orderservice.global.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@AllArgsConstructor
public class Member {

	private final Long id;
	private final String username;
}
