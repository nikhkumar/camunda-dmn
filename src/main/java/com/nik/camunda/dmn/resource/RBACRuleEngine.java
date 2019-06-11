package com.nik.camunda.dmn.resource;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.QueryParam;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.HitPolicy;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.camunda.bpm.model.dmn.instance.DecisionTable;
import org.camunda.bpm.model.dmn.instance.Definitions;
import org.camunda.bpm.model.dmn.instance.Input;
import org.camunda.bpm.model.dmn.instance.InputEntry;
import org.camunda.bpm.model.dmn.instance.InputExpression;
import org.camunda.bpm.model.dmn.instance.Output;
import org.camunda.bpm.model.dmn.instance.OutputEntry;
import org.camunda.bpm.model.dmn.instance.Rule;
import org.camunda.bpm.model.dmn.instance.Text;
import org.camunda.commons.utils.IoUtil;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.nik.camunda.dmn.model.DMNResponse;
import com.nik.camunda.dmn.model.RPResponse;
import com.nik.camunda.dmn.model.RPRule;
import com.nik.camunda.dmn.model.RolePermission;
import com.nik.camunda.dmn.util.Constant;

/**
 * References https://github.com/camunda/camunda-engine-dmn-benchmark
 * https://forum.camunda.org/t/programmatic-management-of-dmn-tables/4240
 * https://stackoverflow.com/questions/56382788/how-to-update-camunda-dmn-table-at-runtime
 * 
 * @git https://github.com/nikhkumar/camunda-dmn.git
 * @author Nikhil Kumar
 */
@RestController
public class RBACRuleEngine {

	String currentDMN = "RBAC_RULES.dmn";
	String outputDMN = "OUT_RBAC_RULES.dmn";

	/**
	 * Deletes the rule for the given role, updates the DMN table and redeploys
	 * again http://localhost:8091/rule?role=DEVELOPER
	 * 
	 * @param role
	 * @return message
	 */
	@RequestMapping(path = "/rule{role}", method = RequestMethod.DELETE)
	public DMNResponse deleteRule(@QueryParam("role") String role) {

		if (role != null && !"".equalsIgnoreCase(role.trim())) {

			InputStream inputStream = IoUtil.fileAsStream(currentDMN);
			DmnModelInstance dmnModelInstance = Dmn.readModelFromStream(inputStream);

			DecisionTable decisionTable = dmnModelInstance.getModelElementById("decisionTableID");

			/** Iterating through all the rules under decision table */
			for (Rule currRule : decisionTable.getRules()) {
				for (InputEntry inEntry : currRule.getInputEntries()) {
					String rawText = inEntry.getRawTextContent();

					/** Removing the rule from decision table if the rawText=requestRole */
					if (rawText != null && role.equalsIgnoreCase((rawText).substring(1, (rawText.length() - 1)))) {
						System.out.println(role + "------" + rawText + " REMOVE");
						decisionTable.getRules().remove(currRule);
					}
				}
			}
			/** Creating a DMN file with same name */
			File dmnFile = new File("src/main/resources/" + currentDMN);
			Dmn.writeModelToFile(dmnFile, dmnModelInstance);

			System.out.println("GENERATE DMN file : " + dmnFile.getAbsolutePath());

			ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
					.buildProcessEngine();
			try {

				/** when instance is deployed via addString method */
				org.camunda.bpm.engine.repository.Deployment deployment = processEngine.getRepositoryService()
						.createDeployment()
						.addString("src/main/resources/" + currentDMN, Dmn.convertToString(dmnModelInstance))
						.name("Deployment after delete " + new Date()).deploy();

			} finally {
				// processEngine.close();
			}
			return prepareResponse(Constant.RES_STATUS_OK, Constant.RES_CODE_OK, "Rule deleted sucuessfully", null);
		} else {
			return prepareResponse(Constant.RES_STATUS_ERR, Constant.RES_CODE_ERR, "Role is missing", null);
		}
	}

	/**
	 * Updates the rule for the given role, updates the DMN table and redeploys
	 * again http://localhost:8091/rule?role=DEVELOPER
	 * 
	 * @param role
	 * @return message
	 */
	@RequestMapping(path = "/rule{role}", method = RequestMethod.PUT)
	public DMNResponse updateRule(@QueryParam("role") String role, @RequestBody RPRule request) {

		if (role != null && !"".equalsIgnoreCase(role.trim())) {

			InputStream inputStream = IoUtil.fileAsStream(currentDMN);
			DmnModelInstance dmnModelInstance = Dmn.readModelFromStream(inputStream);

			DecisionTable decisionTable = dmnModelInstance.getModelElementById("decisionTableID");

			/** Iterating through all the rules under decision table */
			for (Rule currRule : decisionTable.getRules()) {
				for (InputEntry inEntry : currRule.getInputEntries()) {
					String rawText = inEntry.getRawTextContent();

					/** Removing the rule from decision table if the rawText=requestRole */
					if (rawText != null && role.equalsIgnoreCase((rawText).substring(1, (rawText.length() - 1)))) {
						System.out.println("REMOVE " + role + "------" + rawText);
						decisionTable.getRules().remove(currRule);
					}
				}
			}
			/** Adding the new rule sent in the request */
			for (RolePermission rolePer : request.getRules()) {

				String output = String.join(",", rolePer.getPermission());

				System.out.println("ADD " + rolePer.getRole() + " -- " + output);
				Rule newRuleToAdd = createRuleNew(dmnModelInstance, 1, "\"" + rolePer.getRole() + "\"",
						"\"" + output + "\"");
				decisionTable.getRules().add(newRuleToAdd);
			} // for

			/** Creating a DMN file with same name */
			File dmnFile = new File("src/main/resources/" + currentDMN);
			Dmn.writeModelToFile(dmnFile, dmnModelInstance);

			System.out.println("GENERATE DMN file : " + dmnFile.getAbsolutePath());

			ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
					.buildProcessEngine();
			try {

				/** when instance is deployed via addString method */
				org.camunda.bpm.engine.repository.Deployment deployment = processEngine.getRepositoryService()
						.createDeployment()
						.addString("src/main/resources/" + currentDMN, Dmn.convertToString(dmnModelInstance))
						.name("Deployment after update " + new Date()).deploy();
			} finally {
				// processEngine.close();
			}
			return prepareResponse(Constant.RES_STATUS_OK, Constant.RES_CODE_OK, "Rule updated sucuessfully", null);
		} else {
			return prepareResponse(Constant.RES_STATUS_ERR, Constant.RES_CODE_ERR, "Role is missing", null);
		}
	}

	/**
	 * Gets the permissions for the given role
	 * http://localhost:8091/rule?role=DEVELOPER
	 * 
	 * @param role
	 * @return list of permissions
	 */
	@RequestMapping(path = "/rule{role}", method = RequestMethod.GET)
	public DMNResponse getRule(@QueryParam("role") String role) {

		RPResponse ruleResponse = new RPResponse();
		List<String> myList = null;

		ruleResponse.setRole(role);
		myList = new ArrayList<String>();
		myList.add("VIEW");
		ruleResponse.setPermission(myList);

		if (role != null && !"".equalsIgnoreCase(role.trim())) {
			// configure and build the DMN engine
			DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();

			// parse a decision
			InputStream inputStream = IoUtil.fileAsStream("RBAC_RULES.dmn");
			DmnDecision decision = dmnEngine.parseDecision("decisionID", inputStream);

			Map<String, Object> data = new HashMap<String, Object>();
			data.put("role", role.toUpperCase());

			// evaluate a decision
			DmnDecisionTableResult result = dmnEngine.evaluateDecisionTable(decision, data);

			if (result != null && result.getSingleResult() != null) {
				String str = result.getSingleResult().getSingleEntry();
				System.out.println(str);
				myList = new ArrayList<String>(Arrays.asList(str.split(",")));
				ruleResponse.setPermission(myList);
				return prepareResponse(Constant.RES_STATUS_OK, Constant.RES_CODE_OK, null, ruleResponse);
			} else {
				return prepareResponse(Constant.RES_STATUS_OK, Constant.RES_CODE_OK,
						"Role not found, setting to default permission", ruleResponse);
			}
		}
		return prepareResponse(Constant.RES_STATUS_OK, Constant.RES_CODE_OK,
				"Role value missing in request, setting to default permission", ruleResponse);
	}

	/**
	 * Creates a new rule, update the DMn table and redeploys again
	 * 
	 * @param request
	 * @return message
	 */
	@RequestMapping(path = "/rule", method = RequestMethod.POST)
	public DMNResponse createule(@RequestBody RPRule request) {

		StringBuffer sb = new StringBuffer();

		if (request != null) {
			System.out.println(request.toString());

			if (!request.getRules().isEmpty()) {

				InputStream inputStream = IoUtil.fileAsStream(currentDMN);
				DmnModelInstance dmnModelInstance = Dmn.readModelFromStream(inputStream);

				DecisionTable decisionTable = dmnModelInstance.getModelElementById("decisionTableID");

				/** Create new rules based on request data and adding to decision table */
				for (RolePermission rolePer : request.getRules()) {

					sb.append("-" + rolePer.getRole());
					String output = String.join(",", rolePer.getPermission());

					Rule newRuleToAdd = createRuleNew(dmnModelInstance, 1, "\"" + rolePer.getRole() + "\"",
							"\"" + output + "\"");
					decisionTable.getRules().add(newRuleToAdd);
				} // for

				/** Creating a DMN file with same name */
				File dmnFile = new File("src/main/resources/" + currentDMN);
				Dmn.writeModelToFile(dmnFile, dmnModelInstance);

				System.out.println("generate dmn file: " + dmnFile.getAbsolutePath());

				ProcessEngine processEngine = ProcessEngineConfiguration
						.createStandaloneInMemProcessEngineConfiguration().buildProcessEngine();

				try {
					//					      processEngine.getRepositoryService()
					//					        .createDeployment()
					//					        .name("new dmn deployment")
					//					        //.addClasspathResource("OUT_RBAC_RULES.dmn")
					//					        .addInputStream(outputDMN, ClassLoader.getSystemResourceAsStream(outputDMN))
					//					        .deploy();

					/** when instance is deployed via addString method */
					org.camunda.bpm.engine.repository.Deployment deployment = processEngine.getRepositoryService()
							.createDeployment()
							.addString("src/main/resources/" + currentDMN, Dmn.convertToString(dmnModelInstance))
							.name("Deployment after ADD " + sb.toString()).deploy();

					System.out.println("Deployment ID : " + deployment.getId());
				} finally {
					// processEngine.close();
				}
			}
		}
		return prepareResponse(Constant.RES_STATUS_OK, Constant.RES_CODE_OK, "Rule added sucuessfully", null);
	}

	/******************************************************************************/

	private DMNResponse prepareResponse(String resStatus, int resCode, String message, RPResponse ruleResponse) {
		DMNResponse response = new DMNResponse();
		response.setCode(resCode);
		response.setMessage(message);
		response.setStatus(resStatus);
		response.setResult(ruleResponse);
		return response;
	}

	private Rule createRuleNew(DmnModelInstance dmnModelInstance, double numberOfInputs, String in, String out) {

		OutputEntry outputEntry = createOutputEntry(dmnModelInstance, out);

		Rule rule = dmnModelInstance.newInstance(Rule.class);

		for (int i = 0; i < numberOfInputs; i++) {
			InputEntry inputEntry = createInputEntry(dmnModelInstance, in);
			rule.getInputEntries().add(inputEntry);
		}
		rule.getOutputEntries().add(outputEntry);
		return rule;
	}

	public void generateDmn(String template, String output, String decisionId, long numberOfRules,
			long numberOfInputs) {

		// InputStream inputStream = getClass().getResourceAsStream(template);

		InputStream inputStream = IoUtil.fileAsStream("TEMPLATE_ONE_INPUT_RULE.dmn");

		DmnModelInstance dmnModelInstance = Dmn.readModelFromStream(inputStream);

		// set id of the decision
		Decision decision = dmnModelInstance.getModelElementById("templateDecisionID");
		decision.setId(decisionId);

		// add the rules
		DecisionTable decisionTable = dmnModelInstance.getModelElementById("decisionTableID");

		for (int i = 0; i < numberOfRules; i++) {
			//double x = (double) i / numberOfRules;

			Rule rule = createRule(dmnModelInstance, numberOfInputs, "PARTNER");
			decisionTable.getRules().add(rule);

			System.out.println(rule.toString());
		}

		// write the dmn file
		File dmnFile = new File(output);
		Dmn.writeModelToFile(dmnFile, dmnModelInstance);

		System.out.println("generate dmn file: " + dmnFile.getAbsolutePath());
	}

	private Rule createRule(DmnModelInstance dmnModelInstance, double numberOfInputs, String x) {

		OutputEntry outputEntry = createOutputEntry(dmnModelInstance, "NEW VALUE");

		Rule rule = dmnModelInstance.newInstance(Rule.class);

		for (int i = 0; i < numberOfInputs; i++) {
			InputEntry inputEntry = createInputEntry(dmnModelInstance, x);
			rule.getInputEntries().add(inputEntry);
			System.out.println(inputEntry.toString());
		}

		System.out.println(outputEntry.toString());

		rule.getOutputEntries().add(outputEntry);

		return rule;
	}

	private InputEntry createInputEntry(DmnModelInstance dmnModelInstance, String expression) {
		Text text = dmnModelInstance.newInstance(Text.class);
		text.setTextContent(expression);

		InputEntry inputEntry = dmnModelInstance.newInstance(InputEntry.class);
		inputEntry.setText(text);
		return inputEntry;
	}

	private OutputEntry createOutputEntry(DmnModelInstance dmnModelInstance, String expression) {
		Text text = dmnModelInstance.newInstance(Text.class);
		text.setTextContent(expression);

		OutputEntry outputEntry = dmnModelInstance.newInstance(OutputEntry.class);
		outputEntry.setText(text);
		return outputEntry;
	}

	@RequestMapping("/createnew")
	public void createUpdateDMN() {

		DmnModelInstance modelInstance = Dmn.createEmptyModel();
		Definitions definitions = modelInstance.newInstance(Definitions.class);
		definitions.setNamespace("http://camunda.org/schema/1.0/dmn");
		definitions.setName("nameRBACRules");
		definitions.setId("idRBACRules");
		modelInstance.setDefinitions(definitions);

		Decision decision = modelInstance.newInstance(Decision.class);
		decision.setId("rbacRules");
		decision.setName("RBAC_RULES");
		definitions.addChildElement(decision);

		DecisionTable decisionTable = modelInstance.newInstance(DecisionTable.class);
		decisionTable.setId("tblRBACRule");
		decisionTable.setHitPolicy(HitPolicy.FIRST);
		decision.addChildElement(decisionTable);

		Input jahreszeitInput = modelInstance.newInstance(Input.class);
		jahreszeitInput.setId("role");
		jahreszeitInput.setLabel("Role");

		InputExpression inputExpression = modelInstance.newInstance(InputExpression.class);
		inputExpression.setId("InputExpression_1");
		inputExpression.setTypeRef("string");
		Text text = modelInstance.newInstance(Text.class);
		text.setTextContent("role");
		inputExpression.setText(text);
		jahreszeitInput.addChildElement(inputExpression);
		decisionTable.addChildElement(jahreszeitInput);

		//	    Input anzahlGaesteInput = modelInstance.newInstance(Input.class);
		//	    anzahlGaesteInput.setId("Input_2");
		//	    anzahlGaesteInput.setLabel("Number of guests");
		//	    
		//	    InputExpression inputExpression2 = modelInstance.newInstance(InputExpression.class);
		//	    inputExpression2.setId("InputExpression_2");
		//	    inputExpression2.setTypeRef("integer");
		//	    Text text3 = modelInstance.newInstance(Text.class);
		//	    text3.setTextContent("guestCount");
		//	    inputExpression2.setText(text3);
		//	    anzahlGaesteInput.addChildElement(inputExpression2);
		//	    decisionTable.addChildElement(anzahlGaesteInput);

		Output output = modelInstance.newInstance(Output.class);
		output.setId("permission");
		output.setLabel("Permission");
		output.setName("Permission");
		output.setTypeRef("string");
		decisionTable.addChildElement(output);

		Rule rule = modelInstance.newInstance(Rule.class);
		rule.setId("Rule_1");
		Text text1 = modelInstance.newInstance(Text.class);
		text1.setTextContent("\"DEVELOPER\"");
		InputEntry inputEntry = modelInstance.newInstance(InputEntry.class);
		inputEntry.setId("idDEV");
		inputEntry.addChildElement(text1);

		rule.addChildElement(inputEntry);

		//	    Text text4 = modelInstance.newInstance(Text.class);
		//	    text4.setTextContent("<4");
		//	    InputEntry inputEntry2 = modelInstance.newInstance(InputEntry.class);
		//	    inputEntry2.setId("input_4");
		//	    inputEntry2.addChildElement(text4);
		//	    
		//	    rule.addChildElement(inputEntry2);

		OutputEntry outputEntry = modelInstance.newInstance(OutputEntry.class);
		outputEntry.setId("output_2");

		Text text2 = modelInstance.newInstance(Text.class);
		text2.setTextContent("\"VIEW,ADD,UPDATE\"");
		outputEntry.addChildElement(text2);

		rule.addChildElement(outputEntry);

		decisionTable.addChildElement(rule);

		Rule rule2 = modelInstance.newInstance(Rule.class);
		Text text5 = modelInstance.newInstance(Text.class);
		text5.setTextContent("\"ADMIN\"");
		InputEntry inputEntry1 = modelInstance.newInstance(InputEntry.class);
		inputEntry1.setId("input_1");
		inputEntry1.addChildElement(text5);

		rule2.addChildElement(inputEntry1);

		//	    Text text41 = modelInstance.newInstance(Text.class);
		//	    text41.setTextContent(">=4");
		//	    InputEntry inputEntry21 = modelInstance.newInstance(InputEntry.class);
		//	    inputEntry21.setId("input_2");
		//	    inputEntry21.addChildElement(text41);
		//	    
		//	    rule2.addChildElement(inputEntry21);

		OutputEntry outputEntry1 = modelInstance.newInstance(OutputEntry.class);
		outputEntry1.setId("output_1");

		Text text21 = modelInstance.newInstance(Text.class);
		text21.setTextContent("\"VIEW,ADD,UPDATE,DELETE\"");
		outputEntry1.addChildElement(text21);

		rule2.addChildElement(outputEntry1);
		decisionTable.addChildElement(rule2);

		Dmn.validateModel(modelInstance);

		System.out.println(Dmn.convertToString(modelInstance));

		DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();

		DmnDecision decision2 = dmnEngine.parseDecision("rbacRules", modelInstance);

		VariableMap variables = Variables.createVariables().putValue("role", "DEVELOPER");

		DmnDecisionTableResult result = dmnEngine.evaluateDecisionTable(decision2, variables);

		System.out.println(result.toString());

		Collection<Input> inputs = modelInstance.getModelElementsByType(Input.class);
		for (Input input2 : inputs) {
			System.out.println("" + input2.getRawTextContent());
		}
		System.out.println();
		Collection<InputEntry> inputEntries = modelInstance.getModelElementsByType(InputEntry.class);
		for (InputEntry inputEntry3 : inputEntries) {
			System.out.println("" + inputEntry3.getRawTextContent());

		}
	}
}
