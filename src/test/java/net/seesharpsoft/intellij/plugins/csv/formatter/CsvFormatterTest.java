package net.seesharpsoft.intellij.plugins.csv.formatter;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.util.containers.ContainerUtil;
import org.junit.Assert;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.Properties;

public class CsvFormatterTest extends LightCodeInsightFixtureTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected String getTestDataPath() {
        return "./src/test/resources/formatter";
    }

    private void initCsvCodeStyleSettings(boolean SPACE_BEFORE_SEPARATOR,
                                          boolean SPACE_AFTER_SEPARATOR,
                                          boolean TRIM_LEADING_WHITE_SPACES,
                                          boolean TRIM_TRAILING_WHITE_SPACES,
                                          boolean TABULARIZE,
                                          boolean WHITE_SPACES_OUTSIDE_QUOTES,
                                          boolean LEADING_WHITE_SPACES) {
        CsvCodeStyleSettings csvCodeStyleSettings = CodeStyleSettingsManager.getSettings(getProject()).getCustomSettings(CsvCodeStyleSettings.class);
        csvCodeStyleSettings.SPACE_BEFORE_SEPARATOR = SPACE_BEFORE_SEPARATOR;
        csvCodeStyleSettings.SPACE_AFTER_SEPARATOR = SPACE_AFTER_SEPARATOR;
        csvCodeStyleSettings.TRIM_LEADING_WHITE_SPACES = TRIM_LEADING_WHITE_SPACES;
        csvCodeStyleSettings.TRIM_TRAILING_WHITE_SPACES = TRIM_TRAILING_WHITE_SPACES;
        csvCodeStyleSettings.TABULARIZE = TABULARIZE;
        csvCodeStyleSettings.WHITE_SPACES_OUTSIDE_QUOTES = WHITE_SPACES_OUTSIDE_QUOTES;
        csvCodeStyleSettings.LEADING_WHITE_SPACES = LEADING_WHITE_SPACES;
    }

    private void initCsvCodeStyleSettings(int binarySettings) {
        initCsvCodeStyleSettings(
                (binarySettings & 1) != 0,
                (binarySettings & 2) != 0,
                (binarySettings & 4) != 0,
                (binarySettings & 8) != 0,
                (binarySettings & 16) != 0,
                (binarySettings & 32) != 0,
                (binarySettings & 64) != 0);
    }

    private void executeTestConfiguration(int binarySettings, String relativeTargetPath) {
        executeTestConfiguration(binarySettings, relativeTargetPath, true);
    }
    
    private void executeTestConfiguration(int binarySettings, String relativeTargetPath, boolean checkResults) {
        if (relativeTargetPath == null || relativeTargetPath.isEmpty()) {
            relativeTargetPath = ".";
        }

        myFixture.configureByFiles(relativeTargetPath + "/TestData.csv");

        initCsvCodeStyleSettings(binarySettings);

        new WriteCommandAction.Simple(getProject()) {
            @Override
            protected void run() throws Throwable {
                CodeStyleManager.getInstance(getProject()).reformatText(myFixture.getFile(),
                        ContainerUtil.newArrayList(myFixture.getFile().getTextRange()));
            }
        }.execute();
        if (checkResults) {
            myFixture.checkResultByFile(relativeTargetPath + String.format("/TestResult%08d.csv", binarySettings));
        }
    }

    public void testManualFormattedFiles() throws Exception {
        for (int i = 0; i < 5; ++i) {
            tearDown();
            setUp();
            executeTestConfiguration(i, null);
        }
    }

    public void testGeneratedFormattedFiles() throws Exception {
        for (int i = 0; i < 128; ++i) {
            tearDown();
            setUp();
            executeTestConfiguration(i, "/generated");
        }
    }

    public void testFormattedFilesSingle() throws Exception {
        executeTestConfiguration(16, "/header/4");
    }

    public void testSpecialFormattedFiles() throws Exception {
        int[] optionsToTest = new int[]{0, 1, 16, 17, 18, 19, 48, 80, 112};
        for (int i = 0; i < optionsToTest.length; ++i) {
            tearDown();
            setUp();
            executeTestConfiguration(optionsToTest[i], "/special");
        }
    }

    public void testHeader() throws Exception {
        int[] optionsToTest = new int[]{0, 1, 4, 16, 17, 18, 19, 48, 80, 112};
        for (int subTest = 1; subTest < 7; ++subTest) {
            for (int i = 0; i < optionsToTest.length; ++i) {
                tearDown();
                setUp();
                executeTestConfiguration(optionsToTest[i], "/header/" + subTest);
            }
        }
    }
    
    public void testInvalidRangeException() throws Exception {
        executeTestConfiguration(0, "/invalidRange");
    }
    
    public void testRuntimeSimple() throws Exception {
        Long threshold = 1000l;
        Properties runtime = new Properties();
        runtime.load(this.getClass().getResourceAsStream("/formatter/performance/runtime.prop"));
        
        long start = Instant.now().toEpochMilli();
        executeTestConfiguration(0, "/performance", false);
        long end = Instant.now().toEpochMilli();
        System.out.println(end - start);

        Assert.assertTrue(end - start <= Long.parseLong(runtime.getProperty("00000000")) + threshold);
    }
    
    /**
     * This function should be executed (remove the underscore) if the current results are correct (manual testing).
     *
     * @throws Exception
     */
    public void _testResultGenerator() throws Exception {
        for (int binarySettings = 0; binarySettings < 128; ++binarySettings) {
            tearDown();
            setUp();

            myFixture.configureByFiles("/generated/TestData.csv");

            initCsvCodeStyleSettings(binarySettings);

            new WriteCommandAction.Simple(getProject()) {
                @Override
                protected void run() throws Throwable {
                    CodeStyleManager.getInstance(getProject()).reformatText(myFixture.getFile(),
                            ContainerUtil.newArrayList(myFixture.getFile().getTextRange()));
                }
            }.execute();

            try (PrintWriter writer = new PrintWriter(getTestDataPath() + String.format("/generated/TestResult%08d.csv", binarySettings))
            ) {
                writer.print(myFixture.getFile().getText());
            }
        }
    }


}
