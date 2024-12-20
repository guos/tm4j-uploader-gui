package com.uonow.jira.tm4j.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CaseRequest {
	String status;
	String comment;
	//String projectKey;
	//String testCaseKey;
}
