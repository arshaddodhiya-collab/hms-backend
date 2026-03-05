package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.LabRequestCreateRequest;
import com.hms.HospitalManagementSystem.dto.request.LabResultRequest;
import com.hms.HospitalManagementSystem.dto.request.LabTestRequest;
import com.hms.HospitalManagementSystem.entity.LabRequest;
import com.hms.HospitalManagementSystem.entity.LabTestCatalog;
import com.hms.HospitalManagementSystem.enums.LabRequestStatus;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface LabService {
    LabTestCatalog createLabTest(LabTestRequest request);

    Slice<LabTestCatalog> getAllLabTests(
            org.springframework.data.domain.Pageable pageable);

    LabRequest createLabRequest(LabRequestCreateRequest request);

    Slice<LabRequest> getLabQueue(List<LabRequestStatus> statuses,
                                  org.springframework.data.domain.Pageable pageable);

    LabRequest getLabRequestById(Long id);

    Slice<LabRequest> getRequestsByEncounter(Long encounterId,
            org.springframework.data.domain.Pageable pageable);

    LabRequest updateStatus(Long id, LabRequestStatus status);

    LabRequest addLabResults(Long requestId, List<LabResultRequest> resultRequests);
}
