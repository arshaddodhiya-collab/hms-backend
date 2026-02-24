package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.LabRequestCreateRequest;
import com.hms.HospitalManagementSystem.dto.request.LabResultRequest;
import com.hms.HospitalManagementSystem.dto.request.LabTestRequest;
import com.hms.HospitalManagementSystem.entity.LabRequest;
import com.hms.HospitalManagementSystem.entity.LabTestCatalog;
import com.hms.HospitalManagementSystem.enums.LabRequestStatus;

import java.util.List;

public interface LabService {
    LabTestCatalog createLabTest(LabTestRequest request);

    List<LabTestCatalog> getAllLabTests();

    LabRequest createLabRequest(LabRequestCreateRequest request);

    List<LabRequest> getLabQueue(List<LabRequestStatus> statuses);

    LabRequest getLabRequestById(Long id);

    List<LabRequest> getRequestsByEncounter(Long encounterId);

    LabRequest updateStatus(Long id, LabRequestStatus status);

    LabRequest addLabResults(Long requestId, List<LabResultRequest> resultRequests);
}
