package com.hms.HospitalManagementSystem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.hms.HospitalManagementSystem.mapper.EncounterMapper;
import com.hms.HospitalManagementSystem.mapper.PaymentMapper;
import com.hms.HospitalManagementSystem.mapper.InvoiceMapper;
import com.hms.HospitalManagementSystem.mapper.IpdMapper;

@SpringBootTest
class HospitalManagementSystemApplicationTests {

	@MockBean
	private EncounterMapper encounterMapper;
	@MockBean
	private PaymentMapper paymentMapper;
	@MockBean
	private InvoiceMapper invoiceMapper;
	@MockBean
	private IpdMapper ipdMapper;

	@Test
	void contextLoads() {
	}

}
