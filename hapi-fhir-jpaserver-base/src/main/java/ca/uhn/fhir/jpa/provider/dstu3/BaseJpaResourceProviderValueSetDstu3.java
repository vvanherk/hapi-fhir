package ca.uhn.fhir.jpa.provider.dstu3;

/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2016 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.dstu3.model.ValueSet;

import ca.uhn.fhir.jpa.dao.IFhirResourceDaoValueSet;
import ca.uhn.fhir.jpa.dao.IFhirResourceDaoValueSet.ValidateCodeResult;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

public class BaseJpaResourceProviderValueSetDstu3 extends JpaResourceProviderDstu3<ValueSet> {

	//@formatter:off
	@Operation(name = "$expand", idempotent = true)
	public ValueSet expand(
			HttpServletRequest theServletRequest,
			@IdParam(optional=true) IdType theId,
			@OperationParam(name="valueSet", min=0, max=1) ValueSet theValueSet,
			@OperationParam(name="identifier", min=0, max=1) UriType theIdentifier,
			@OperationParam(name = "filter", min=0, max=1) StringType theFilter) {
		//@formatter:on
		
		boolean haveId = theId != null && theId.hasIdPart();
		boolean haveIdentifier = theIdentifier != null && isNotBlank(theIdentifier.getValue());
		boolean haveValueSet = theValueSet != null && theValueSet.isEmpty() == false;
		
		if (!haveId && !haveIdentifier && !haveValueSet) {
			throw new InvalidRequestException("$expand operation at the type level (no ID specified) requires an identifier or a valueSet as a part of the request");
		}

		if (moreThanOneTrue(haveId, haveIdentifier, haveValueSet)) {
			throw new InvalidRequestException("$expand must EITHER be invoked at the instance level, or have an identifier specified, or have a ValueSet specified. Can not combine these options.");
		}
		
		startRequest(theServletRequest);
		try {
			IFhirResourceDaoValueSet<ValueSet, Coding, CodeableConcept> dao = (IFhirResourceDaoValueSet<ValueSet, Coding, CodeableConcept>) getDao();
			if (haveId) {
				return dao.expand(theId, toFilterString(theFilter));
			} else if (haveIdentifier) {
				return dao.expandByIdentifier(theIdentifier.getValue(), toFilterString(theFilter));
			} else {
				return dao.expand(theValueSet, toFilterString(theFilter));
			}
			
		} finally {
			endRequest(theServletRequest);
		}
	}


	private static boolean moreThanOneTrue(boolean... theBooleans) {
		boolean haveOne = false;
		for (boolean next : theBooleans) {
			if (next) {
				if (haveOne) {
					return true;
				} else {
					haveOne = true;
				}
			}
		}
		return false;
	}


	private String toFilterString(StringType theFilter) {
		return theFilter != null ? theFilter.getValue() : null;
	}

	
	
	//@formatter:off
	@SuppressWarnings("unchecked")
	@Operation(name = "$validate-code", idempotent = true, returnParameters= {
		@OperationParam(name="result", type=BooleanType.class, min=1),
		@OperationParam(name="message", type=StringType.class),
		@OperationParam(name="display", type=StringType.class)
	})
	public Parameters validateCode(
			HttpServletRequest theServletRequest,
			@IdParam(optional=true) IdType theId, 
			@OperationParam(name="identifier", min=0, max=1) UriType theValueSetIdentifier, 
			@OperationParam(name="code", min=0, max=1) CodeType theCode, 
			@OperationParam(name="system", min=0, max=1) UriType theSystem,
			@OperationParam(name="display", min=0, max=1) StringType theDisplay,
			@OperationParam(name="coding", min=0, max=1) Coding theCoding,
			@OperationParam(name="codeableConcept", min=0, max=1) CodeableConcept theCodeableConcept
			) {
		//@formatter:on
		
		startRequest(theServletRequest);
		try {
			IFhirResourceDaoValueSet<ValueSet, Coding, CodeableConcept> dao = (IFhirResourceDaoValueSet<ValueSet, Coding, CodeableConcept>) getDao();
			ValidateCodeResult result = dao.validateCode(theValueSetIdentifier, theId, theCode, theSystem, theDisplay, theCoding, theCodeableConcept);
			Parameters retVal = new Parameters();
			retVal.addParameter().setName("result").setValue(new BooleanType(result.isResult()));
			if (isNotBlank(result.getMessage())) {
				retVal.addParameter().setName("message").setValue(new StringType(result.getMessage()));
			}
			if (isNotBlank(result.getDisplay())) {
				retVal.addParameter().setName("display").setValue(new StringType(result.getDisplay()));
			}
			return retVal;
		} finally {
			endRequest(theServletRequest);
		}
	}

	
}