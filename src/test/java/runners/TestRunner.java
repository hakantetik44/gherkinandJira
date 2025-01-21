package runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features",
    glue = "stepdefinitions",
    plugin = {
        "pretty",
        "json:target/cucumber-reports/cucumber.json",
        "html:target/cucumber-reports/cucumber.html",
        "junit:target/cucumber-reports/cucumber.xml",
        "rerun:target/failed_scenarios.txt"
    },
    monochrome = true,
    dryRun = false,
    tags = "@test"
)
public class TestRunner {
} 