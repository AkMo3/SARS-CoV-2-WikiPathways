package org.wikipathways.covid;

import java.io.File;
import java.io.FileInputStream;

import java.util.ArrayList;
import java.util.List;

import nl.unimaas.bigcat.wikipathways.curator.assertions.*;
import nl.unimaas.bigcat.wikipathways.curator.tests.*;
import nl.unimaas.bigcat.wikipathways.curator.SPARQLHelper;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class CheckRDF {

    public static String getHashcode(String string) { return Integer.toHexString(string.hashCode()); }

    public static void main(String[] args) throws Exception {
        String wpFile   = args[0];
        String gpmlFile = wpFile.replace("wp/Human", "wp/gpml/Human");
        String wpid     = wpFile.substring(9,wpFile.indexOf(".ttl"));
        System.out.println("# WikiPathways " + wpid + "\n");
        System.out.println("* WikiPathways: [" + wpid + "](https://identifiers.org/wikipathways:" + wpid + ")");
        System.out.println("* Scholia: [" + wpid + "](https://scholia.toolforge.org/wikipathways/" + wpid + ")");
        System.out.println("* WPRDF file: [" + wpFile + "](../" + wpFile + ")");
        System.out.println("* GPMLRDF file: [" + gpmlFile + "](../" + gpmlFile + ")\n");
        List<IAssertion> assertions = new ArrayList<IAssertion>();
        Model loadedData = ModelFactory.createDefaultModel();
        loadedData.read(new FileInputStream(new File(wpFile)), "", "TURTLE");
        loadedData.read(new FileInputStream(new File(gpmlFile)), "", "TURTLE");

        SPARQLHelper helper = new SPARQLHelper(loadedData);
        assertions.addAll(GeneralTests.all(helper));
        assertions.addAll(ReferencesTests.all(helper));

        assertions.addAll(CovidDiseaseMapsTests.all(helper));

        assertions.addAll(GeneTests.all(helper));
        assertions.addAll(EnsemblTests.all(helper));

        assertions.addAll(CASMetabolitesTests.all(helper));
        assertions.addAll(ChEBIMetabolitesTests.all(helper));
        assertions.addAll(HMDBMetabolitesTests.outdatedIdentifiers(helper)); // not all
        assertions.addAll(HMDBSecMetabolitesTests.all(helper));
        assertions.addAll(LIPIDMAPSTests.all(helper));
        assertions.addAll(MetabolitesTests.all(helper));

        assertions.addAll(InteractionTests.all(helper));

        System.out.println("## Tests");

        List<IAssertion> failedAssertions = new ArrayList<IAssertion>();
        int testClasses = 0;
        int tests = 0;
        String currentTestClass = "";
        String currentTest = "";
        boolean currentTestClassHasFails = false;
        String message = "";
        String errors = "";
        int errorCount = 0;
        for (IAssertion assertion : assertions) {
            // new test class ?
            if (assertion.getTestClass() != currentTestClass) {
                currentTestClass = assertion.getTestClass();
                currentTest = "";
                testClasses++;
                if (!message.isEmpty()) {
                  if (currentTestClassHasFails) {
                    if (errorCount == 0) { message += " all OK!"; } else { message += " we found " + errorCount + " problem(s):"; }
                    if (!errors.isEmpty()) message += "\n" + errors;
                    System.out.println("\n" + message);
                  } else {
                    System.out.println(" all OK!");
                  }
                }
                message = "";
                System.out.print("\n* " + currentTestClass);
                currentTestClassHasFails = false;
            }

            // new test ?
            if (assertion.getTest() != currentTest) {
                currentTest = assertion.getTest();
                if (!message.isEmpty()) {
                  if (errorCount == 0) { message += " all OK!"; } else { message += " we found " + errorCount + " problem(s):"; }
                  if (!errors.isEmpty()) message += "\n" + errors;
                }
                message = "    * " + currentTest + ": ";
                errorCount = 0;
                errors = "";
                tests++;
            }

            if (assertion instanceof AssertEquals) {
                AssertEquals typedAssertion = (AssertEquals)assertion;
                if (!typedAssertion.getExpectedValue().equals(typedAssertion.getValue())) {
                   message += "x";
                   errorCount++;
                   errors += "        * [" + typedAssertion.getMessage() + "](#" + getHashcode(assertion.getTestClass() + assertion.getTest() + assertion.getMessage()) + ")";
                   failedAssertions.add(assertion);
                   currentTestClassHasFails = true;
                } else {
                    message += ".";
                }
            } else if (assertion instanceof AssertNotSame) {
                AssertNotSame typedAssertion = (AssertNotSame)assertion;
                if (typedAssertion.getExpectedValue().equals(typedAssertion.getValue())) {
                   message += "x";
                   errorCount++;
                   errors += "        * [" + typedAssertion.getMessage() + "](#" + getHashcode(assertion.getTestClass() + assertion.getTest() + assertion.getMessage()) + ")";
                   failedAssertions.add(assertion);
                   currentTestClassHasFails = true;
                } else {
                    message += ".";
                }
            } else if (assertion instanceof AssertNotNull) {
                AssertNotNull typedAssertion = (AssertNotNull)assertion;
                if (typedAssertion.getValue() == null) {
                   message += "x";
                   errorCount++;
                   errors += "            * Unexpected null found";
                   failedAssertions.add(assertion);
                   currentTestClassHasFails = true;
                } else {
                    message += ".";
                }
            } else if (assertion instanceof AssertTrue) {
                AssertTrue typedAssertion = (AssertTrue)assertion;
                if ((boolean)typedAssertion.getValue()) {
                   message += "x";
                   errorCount++;
                   errors += "            * Expected true but found false";
                   failedAssertions.add(assertion);
                   currentTestClassHasFails = true;
                } else {
                    message += ".";
                }
            } else {
                errors += "        * Unrecognized assertion type: " + assertion.getClass().getName();
                failedAssertions.add(assertion);
            }
        }
        if (!message.isEmpty())  {
            if (errorCount == 0) { message += " all OK!"; } else { message += " we found " + errorCount + " problem(s):"; }
            if (!errors.isEmpty()) message += "\n" + errors;
            System.out.print(message);
        }
        System.out.println();
        System.out.println("\n## Summary\n");
        System.out.println("* Number of test classes: " + testClasses);
        System.out.println("* Number of tests: " + tests);
        System.out.println("* Number of assertions: " + assertions.size());
        System.out.println("* Number of fails: " + failedAssertions.size());

        System.out.println("\n## Fails\n");
        for (IAssertion assertion : failedAssertions) {
            System.out.println("<a name=\"" + getHashcode(assertion.getTestClass() + assertion.getTest() + assertion.getMessage()) + "\" />\n");
            System.out.println("## " + assertion.getTestClass() + "." + assertion.getTest());
            System.out.println("\n" + assertion.getMessage());
            if (assertion.getDetails() != null && !assertion.getDetails().isEmpty()) {
                System.out.println("```\n" + assertion.getDetails() + "\n```");
            }
        }
    }

}
