package com.hms.HospitalManagementSystem.specification;

import com.hms.HospitalManagementSystem.entity.Patient;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class PatientSpecification {

    public static Specification<Patient> search(String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (!StringUtils.hasText(query)) {
                return criteriaBuilder.conjunction();
            }
            String likePattern = "%" + query.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("contact")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likePattern));
        };
    }

    public static Specification<Patient> hasContact(String contact) {
        return (root, query, cb) -> StringUtils.hasText(contact) ? cb.equal(root.get("contact"), contact)
                : cb.conjunction();
    }
}
