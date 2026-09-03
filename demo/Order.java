import java.util.List;
import java.util.ArrayList;

class Order {
    private final List<String> items = new ArrayList<>();
    public List<String> getItems() { return items; }
}

class OrderClient {
    void tamper(Order order) {
        List<String> items = order.getItems();
        items.add("extra-item");
    }
}
