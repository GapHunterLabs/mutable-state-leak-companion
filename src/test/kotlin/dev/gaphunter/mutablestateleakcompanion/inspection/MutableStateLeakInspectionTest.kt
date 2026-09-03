package dev.gaphunter.mutablestateleakcompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MutableStateLeakInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(MutableStateLeakInspection::class.java)
    }

    fun `test a caller in a different class that mutates a directly-returned list is flagged`() {
        myFixture.configureByText(
            "Order1.java",
            """
            import java.util.List;
            import java.util.ArrayList;

            class Order1 {
                private final List<String> items = new ArrayList<>();
                public List<String> getItems() { return items; }
            }

            class OrderClient1 {
                void tamper(Order1 order) {
                    List<String> items = order.getItems();
                    items.add("extra-item");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("CWE-374/375") == true && it.description?.contains("Order1.items") == true })
    }

    fun `test a caller that only reads the returned list is not flagged`() {
        myFixture.configureByText(
            "Order2.java",
            """
            import java.util.List;
            import java.util.ArrayList;

            class Order2 {
                private final List<String> items = new ArrayList<>();
                public List<String> getItems() { return items; }
            }

            class OrderClient2 {
                int count(Order2 order) {
                    List<String> items = order.getItems();
                    return items.size();
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("CWE-374/375") == true })
    }

    fun `test a getter that wraps the field in an unmodifiable view is not flagged`() {
        myFixture.configureByText(
            "Order3.java",
            """
            import java.util.List;
            import java.util.ArrayList;
            import java.util.Collections;

            class Order3 {
                private final List<String> items = new ArrayList<>();
                public List<String> getItems() { return Collections.unmodifiableList(items); }
            }

            class OrderClient3 {
                void tamper(Order3 order) {
                    List<String> items = order.getItems();
                    items.add("extra-item");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("CWE-374/375") == true })
    }

    fun `test a getter returning a non-mutable type is not flagged`() {
        myFixture.configureByText(
            "Order4.java",
            """
            class Order4 {
                private final String name = "order";
                public String getName() { return name; }
            }

            class OrderClient4 {
                void run(Order4 order) {
                    String name = order.getName();
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("CWE-374/375") == true })
    }

    fun `test an array field mutated via index write is flagged`() {
        myFixture.configureByText(
            "Board1.java",
            """
            class Board1 {
                private final int[] cells = new int[9];
                public int[] getCells() { return cells; }
            }

            class BoardClient1 {
                void tamper(Board1 board) {
                    int[] cells = board.getCells();
                    cells[0] = 1;
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("CWE-374/375") == true && it.description?.contains("Board1.cells") == true })
    }
}
