package dia03.exercicio10;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        ProductRepository productRepo = new ProductRepository();
        CustomerRepository customerRepo = new CustomerRepository();
        OrderRepository orderRepo = new OrderRepository();

        ProductService productService = new ProductService(productRepo);
        CustomerService customerService = new CustomerService(customerRepo);
        OrderService orderService = new OrderService(orderRepo, productRepo, customerRepo);
        ReportService reportService = new ReportService(orderRepo, productRepo);

        new CommandLineInterface(productService, customerService, orderService, reportService).start();
    }

    public enum OrderStatus {
        CREATED, RESERVED, PAID, FAILED, CANCELLED
    }

    // =====================================================================
    // EXCEÇÕES
    // =====================================================================

    public static class DomainException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public DomainException(String message) { super(message); }
    }

    public static class NotFoundException extends DomainException {
        private static final long serialVersionUID = 1L;
        public NotFoundException(String message) { super(message); }
    }

    public static class InvalidOperationException extends DomainException {
        private static final long serialVersionUID = 1L;
        public InvalidOperationException(String message) { super(message); }
    }

    // =====================================================================
    // MODELOS
    // =====================================================================

    public static class Product {
        private final String sku;
        private final String name;
        private final String category;
        private final BigDecimal price;
        private int stock;
        private int reservedStock;

        public Product(String sku, String name, String category, BigDecimal price, int stock) {
            if (sku == null || sku.isBlank())
                throw new IllegalArgumentException("SKU não pode ser vazio");
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Nome não pode ser vazio");
            if (category == null || category.isBlank())
                throw new IllegalArgumentException("Categoria não pode ser vazia");
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Preço deve ser maior que zero");
            if (stock < 0)
                throw new IllegalArgumentException("Estoque não pode ser negativo");
            this.sku = sku;
            this.name = name;
            this.category = category;
            this.price = price;
            this.stock = stock;
            this.reservedStock = 0;
        }

        public String getSku() { return sku; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public BigDecimal getPrice() { return price; }
        public int getStock() { return stock; }
        public int getReservedStock() { return reservedStock; }
        public int getAvailableStock() { return stock - reservedStock; }

        public void reserve(int quantity) {
            if (quantity <= 0) throw new IllegalArgumentException("Quantidade deve ser positiva");
            if (getAvailableStock() < quantity) {
                throw new IllegalStateException(
                    "Estoque insuficiente para SKU " + sku +
                    " (disponível: " + getAvailableStock() + ", solicitado: " + quantity + ")");
            }
            reservedStock += quantity;
        }

        public void releaseReservation(int quantity) {
            if (quantity <= 0) throw new IllegalArgumentException("Quantidade deve ser positiva");
            if (reservedStock < quantity)
                throw new IllegalStateException("Não há estoque reservado suficiente para liberar");
            reservedStock -= quantity;
        }

        public void confirmReservation(int quantity) {
            if (quantity <= 0) throw new IllegalArgumentException("Quantidade deve ser positiva");
            if (reservedStock < quantity)
                throw new IllegalStateException("Reserva insuficiente para confirmar");
            reservedStock -= quantity;
            stock -= quantity;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Product p)) return false;
            return sku.equals(p.sku);
        }
        @Override public int hashCode() { return Objects.hash(sku); }

        @Override public String toString() {
            return String.format("[%s] %s | Categoria: %s | Preço: R$ %.2f | Estoque: %d (reservado: %d)",
                sku, name, category, price, stock, reservedStock);
        }
    }

    /** Cliente do e-commerce. */
    public static class Customer {
        private final String id;
        private final String name;
        private final String email;

        public Customer(String id, String name, String email) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("ID não pode ser vazio");
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Nome não pode ser vazio");
            if (email == null || !email.contains("@")) throw new IllegalArgumentException("E-mail inválido");
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Customer c)) return false;
            return id.equals(c.id);
        }
        @Override public int hashCode() { return Objects.hash(id); }

        @Override public String toString() {
            return String.format("[%s] %s <%s>", id, name, email);
        }
    }

    /** Item de um pedido */
    public record OrderItem(String sku, int quantity, BigDecimal unitPrice) {
        public OrderItem {
            if (sku == null || sku.isBlank())
                throw new IllegalArgumentException("SKU é obrigatório");
            if (quantity <= 0)
                throw new IllegalArgumentException("Quantidade deve ser positiva");
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Preço unitário inválido");
        }

        public BigDecimal subtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /** Pedido com seus itens, status e desconto. */
    public static class Order {
        private final String id;
        private final String customerId;
        private final List<OrderItem> items;
        private final LocalDateTime createdAt;
        private OrderStatus status;
        private BigDecimal discount;

        public Order(String id, String customerId, List<OrderItem> items) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("ID do pedido é obrigatório");
            if (customerId == null || customerId.isBlank()) throw new IllegalArgumentException("ID do cliente é obrigatório");
            if (items == null || items.isEmpty()) throw new IllegalArgumentException("Pedido deve ter ao menos um item");
            this.id = id;
            this.customerId = customerId;
            this.items = List.copyOf(items);
            this.createdAt = LocalDateTime.now();
            this.status = OrderStatus.CREATED;
            this.discount = BigDecimal.ZERO;
        }

        public String getId() { return id; }
        public String getCustomerId() { return customerId; }
        public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public OrderStatus getStatus() { return status; }
        public BigDecimal getDiscount() { return discount; }
        public void setStatus(OrderStatus newStatus) { this.status = Objects.requireNonNull(newStatus); }

        public void applyDiscount(BigDecimal discount) {
            if (discount == null || discount.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("Desconto inválido");
            if (discount.compareTo(getSubtotal()) > 0)
                throw new IllegalArgumentException("Desconto não pode exceder o subtotal");
            this.discount = discount;
        }

        public BigDecimal getSubtotal() {
            return items.stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public BigDecimal getTotal() {
            return getSubtotal().subtract(discount).setScale(2, RoundingMode.HALF_UP);
        }

        @Override public String toString() {
            return String.format("Pedido %s | Cliente: %s | Status: %s | Itens: %d | Total: R$ %.2f",
                id, customerId, status, items.size(), getTotal());
        }
    }

    // =====================================================================
    // REPOSITÓRIOS
    // =====================================================================

    public static class ProductRepository {
        private final Map<String, Product> products = new HashMap<>();

        public void save(Product product) {
            if (products.containsKey(product.getSku()))
                throw new IllegalArgumentException("SKU já cadastrado: " + product.getSku());
            products.put(product.getSku(), product);
        }
        public Product findBySku(String sku) {
            Product p = products.get(sku);
            if (p == null) throw new NotFoundException("Produto não encontrado: " + sku);
            return p;
        }
        public Optional<Product> findOptional(String sku) { return Optional.ofNullable(products.get(sku)); }
        public List<Product> findAll() { return new ArrayList<>(products.values()); }
        public void remove(String sku) {
            if (products.remove(sku) == null) throw new NotFoundException("Produto não encontrado: " + sku);
        }
        public int count() { return products.size(); }
    }

    public static class CustomerRepository {
        private final Map<String, Customer> customers = new HashMap<>();

        public void save(Customer customer) {
            if (customers.containsKey(customer.getId()))
                throw new IllegalArgumentException("ID de cliente já cadastrado: " + customer.getId());
            customers.put(customer.getId(), customer);
        }
        public Customer findById(String id) {
            Customer c = customers.get(id);
            if (c == null) throw new NotFoundException("Cliente não encontrado: " + id);
            return c;
        }
        public List<Customer> findAll() { return new ArrayList<>(customers.values()); }
        public int count() { return customers.size(); }
    }

    public static class OrderRepository {
        private final Map<String, Order> orders = new LinkedHashMap<>();

        public void save(Order order) { orders.put(order.getId(), order); }
        public Order findById(String id) {
            Order o = orders.get(id);
            if (o == null) throw new NotFoundException("Pedido não encontrado: " + id);
            return o;
        }
        public List<Order> findAll() { return new ArrayList<>(orders.values()); }
        public int count() { return orders.size(); }
    }

    // =====================================================================
    // SERVIÇOS
    // =====================================================================

    public static class ProductService {
        public enum SortBy { SKU, PRICE }

        private final ProductRepository repository;
        public ProductService(ProductRepository repository) { this.repository = repository; }

        public Product addProduct(String sku, String name, String category, BigDecimal price, int stock) {
            Product product = new Product(sku, name, category, price, stock);
            repository.save(product);
            return product;
        }

        public List<Product> listProducts(SortBy sortBy) {
            Comparator<Product> comparator = switch (sortBy) {
                case SKU   -> Comparator.comparing(Product::getSku);
                case PRICE -> Comparator.comparing(Product::getPrice);
            };
            return repository.findAll().stream().sorted(comparator).toList();
        }

        public void removeProduct(String sku) {
            repository.remove(sku);
        }

        public Product findBySku(String sku) { return repository.findBySku(sku); }
    }

    public static class CustomerService {
        private final CustomerRepository repository;
        public CustomerService(CustomerRepository repository) { this.repository = repository; }

        public Customer addCustomer(String id, String name, String email) {
            Customer customer = new Customer(id, name, email);
            repository.save(customer);
            return customer;
        }
        public List<Customer> listAll() { return repository.findAll(); }
        public Customer findById(String id) { return repository.findById(id); }
    }

    public static class OrderService {
        private final OrderRepository orderRepo;
        private final ProductRepository productRepo;
        private final CustomerRepository customerRepo;
        private final AtomicInteger orderCounter = new AtomicInteger(1000);

        public OrderService(OrderRepository orderRepo, ProductRepository productRepo, CustomerRepository customerRepo) {
            this.orderRepo = orderRepo;
            this.productRepo = productRepo;
            this.customerRepo = customerRepo;
        }

        /** Cria um pedido (status CREATED). Valida cliente e produtos. Não reserva estoque ainda. */
        public Order createOrder(String customerId, Map<String, Integer> itemsInput) {
            customerRepo.findById(customerId);
            if (itemsInput == null || itemsInput.isEmpty())
                throw new InvalidOperationException("Pedido precisa de pelo menos um item");

            List<OrderItem> items = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : itemsInput.entrySet()) {
                Product product = productRepo.findBySku(entry.getKey());
                int qty = entry.getValue();
                items.add(new OrderItem(product.getSku(), qty, product.getPrice()));
            }
            String orderId = "ORD-" + orderCounter.incrementAndGet();
            Order order = new Order(orderId, customerId, items);
            orderRepo.save(order);
            return order;
        }

        /** CREATED -> RESERVED. Faz rollback se algum item falhar. */
        public Order reserveStock(String orderId) {
            Order order = orderRepo.findById(orderId);
            if (order.getStatus() != OrderStatus.CREATED)
                throw new InvalidOperationException(
                    "Só é possível reservar pedidos em CREATED. Status atual: " + order.getStatus());

            List<OrderItem> reserved = new ArrayList<>();
            try {
                for (OrderItem item : order.getItems()) {
                    productRepo.findBySku(item.sku()).reserve(item.quantity());
                    reserved.add(item);
                }
            } catch (RuntimeException e) {
                for (OrderItem item : reserved) {
                    productRepo.findBySku(item.sku()).releaseReservation(item.quantity());
                }
                throw new InvalidOperationException("Falha ao reservar estoque: " + e.getMessage());
            }
            order.setStatus(OrderStatus.RESERVED);
            return order;
        }

        public Order applyDiscount(String orderId, BigDecimal discount) {
            Order order = orderRepo.findById(orderId);
            if (order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.FAILED) {
                throw new InvalidOperationException(
                    "Não é possível aplicar desconto. Status atual: " + order.getStatus());
            }
            order.applyDiscount(discount);
            return order;
        }

        /** Pagamento simulado: ~80% sucesso. RESERVED -> PAID ou FAILED. */
        public Order payOrder(String orderId) {
            Order order = orderRepo.findById(orderId);
            if (order.getStatus() != OrderStatus.RESERVED)
                throw new InvalidOperationException(
                    "Só é possível pagar pedidos RESERVED. Status atual: " + order.getStatus());

            boolean approved = ThreadLocalRandom.current().nextDouble() < 0.8;
            if (approved) {
                for (OrderItem item : order.getItems()) {
                    productRepo.findBySku(item.sku()).confirmReservation(item.quantity());
                }
                order.setStatus(OrderStatus.PAID);
            } else {
                for (OrderItem item : order.getItems()) {
                    productRepo.findBySku(item.sku()).releaseReservation(item.quantity());
                }
                order.setStatus(OrderStatus.FAILED);
            }
            return order;
        }

        /** Cancela e libera estoque reservado. Permitido em CREATED ou RESERVED. */
        public Order cancelOrder(String orderId) {
            Order order = orderRepo.findById(orderId);
            if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.RESERVED)
                throw new InvalidOperationException(
                    "Não é possível cancelar pedido com status: " + order.getStatus());

            if (order.getStatus() == OrderStatus.RESERVED) {
                for (OrderItem item : order.getItems()) {
                    productRepo.findBySku(item.sku()).releaseReservation(item.quantity());
                }
            }
            order.setStatus(OrderStatus.CANCELLED);
            return order;
        }

        public List<Order> listAll() { return orderRepo.findAll(); }
        public Order findById(String id) { return orderRepo.findById(id); }
    }

    // =====================================================================
    // RELATÓRIOS
    // =====================================================================

    public static class ReportService {
        public record ProductSales(String sku, String name, int quantitySold) {}
        public record CustomerOrderCount(String customerId, long orderCount) {}

        private final OrderRepository orderRepo;
        private final ProductRepository productRepo;

        public ReportService(OrderRepository orderRepo, ProductRepository productRepo) {
            this.orderRepo = orderRepo;
            this.productRepo = productRepo;
        }

        public BigDecimal totalRevenuePaid() {
            return orderRepo.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public List<ProductSales> top3BestSellers() {
            Map<String, Integer> totalsBySku = new HashMap<>();
            orderRepo.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .flatMap(o -> o.getItems().stream())
                .forEach(item -> totalsBySku.merge(item.sku(), item.quantity(), Integer::sum));

            return totalsBySku.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(e -> {
                    Product p = productRepo.findBySku(e.getKey());
                    return new ProductSales(p.getSku(), p.getName(), e.getValue());
                })
                .toList();
        }

        public Map<String, BigDecimal> revenueByCategory() {
            Map<String, BigDecimal> result = new TreeMap<>();
            orderRepo.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .flatMap(o -> o.getItems().stream())
                .forEach(item -> {
                    Product p = productRepo.findBySku(item.sku());
                    result.merge(p.getCategory(), item.subtotal(), BigDecimal::add);
                });
            return result;
        }

        public List<CustomerOrderCount> customersByOrderCount() {
            Map<String, Long> counts = orderRepo.findAll().stream()
                .collect(Collectors.groupingBy(Order::getCustomerId, Collectors.counting()));

            return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new CustomerOrderCount(e.getKey(), e.getValue()))
                .toList();
        }
    }

    // =====================================================================
    // CLI
    // =====================================================================

    public static class CommandLineInterface {
        private final ProductService productService;
        private final CustomerService customerService;
        private final OrderService orderService;
        private final ReportService reportService;
        private final Scanner scanner;
        private boolean running = true;

        public CommandLineInterface(ProductService p, CustomerService c, OrderService o, ReportService r) {
            this.productService = p;
            this.customerService = c;
            this.orderService = o;
            this.reportService = r;
            this.scanner = new Scanner(System.in);
        }

        public void start() {
            printBanner();
            printHelp();
            while (running) {
                System.out.print("\n> ");
                if (!scanner.hasNextLine()) break;
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                try {
                    handleCommand(line);
                } catch (DomainException e) {
                    System.out.println("[X] " + e.getMessage());
                } catch (IllegalArgumentException | IllegalStateException e) {
                    System.out.println("[X] Erro: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("[X] Erro inesperado: " + e.getMessage());
                }
            }
            System.out.println("Sistema encerrado. Ate logo!");
        }

        private void handleCommand(String line) {
            String[] parts = line.split("\\s+", 2);
            String cmd = parts[0].toLowerCase();
            String args = parts.length > 1 ? parts[1] : "";

            switch (cmd) {
                case "help", "ajuda"        -> printHelp();
                case "add-product"          -> handleAddProduct(args);
                case "list-products"        -> handleListProducts(args);
                case "delete-product"       -> handleDeleteProduct(args);
                case "add-customer"         -> handleAddCustomer(args);
                case "list-customers"       -> handleListCustomers();
                case "create-order"         -> handleCreateOrder(args);
                case "reserve"              -> handleReserve(args);
                case "discount"             -> handleDiscount(args);
                case "pay"                  -> handlePay(args);
                case "cancel"               -> handleCancel(args);
                case "list-orders"          -> handleListOrders();
                case "report"               -> handleReport(args);
                case "demo"                 -> loadDemoData();
                case "exit", "sair", "quit" -> running = false;
                default -> System.out.println("[X] Comando desconhecido: '" + cmd + "'. Digite 'help'.");
            }
        }

        private void handleAddProduct(String args) {
            List<String> tokens = parseTokens(args);
            if (tokens.size() != 5) {
                System.out.println("Uso: add-product <sku> <\"nome\"> <categoria> <preco> <estoque>");
                System.out.println("Ex.: add-product SKU01 \"Notebook Dell\" eletronicos 3500.00 10");
                return;
            }
            Product p = productService.addProduct(
                tokens.get(0), tokens.get(1), tokens.get(2),
                new BigDecimal(tokens.get(3)), Integer.parseInt(tokens.get(4)));
            System.out.println("[OK] Produto cadastrado: " + p);
        }
        
        private void handleDeleteProduct(String args) {
            String sku = args.trim();
            if (sku.isEmpty()) {
                System.out.println("Uso: delete-product <sku>");
                return;
            }
            try {
                productService.removeProduct(sku); 
                System.out.println("[OK] Produto removido com sucesso: " + sku);
            } catch (NotFoundException e) {
                System.out.println("[X] Erro: " + e.getMessage());
            }
        }

        private void handleListProducts(String args) {
            ProductService.SortBy sortBy = args.equalsIgnoreCase("price")
                ? ProductService.SortBy.PRICE : ProductService.SortBy.SKU;
            List<Product> products = productService.listProducts(sortBy);
            if (products.isEmpty()) { System.out.println("(nenhum produto cadastrado)"); return; }
            System.out.println("Produtos (ordenados por " + sortBy + "):");
            products.forEach(p -> System.out.println("  " + p));
        }

        private void handleAddCustomer(String args) {
            List<String> tokens = parseTokens(args);
            if (tokens.size() != 3) {
                System.out.println("Uso: add-customer <id> <\"nome\"> <email>");
                System.out.println("Ex.: add-customer C01 \"Maria Silva\" maria@email.com");
                return;
            }
            Customer c = customerService.addCustomer(tokens.get(0), tokens.get(1), tokens.get(2));
            System.out.println("[OK] Cliente cadastrado: " + c);
        }

        private void handleListCustomers() {
            List<Customer> all = customerService.listAll();
            if (all.isEmpty()) { System.out.println("(nenhum cliente cadastrado)"); return; }
            System.out.println("Clientes:");
            all.forEach(c -> System.out.println("  " + c));
        }

        private void handleCreateOrder(String args) {
            List<String> tokens = parseTokens(args);
            if (tokens.size() < 2) {
                System.out.println("Uso: create-order <customerId> <sku1:qtd1> [sku2:qtd2 ...]");
                System.out.println("Ex.: create-order C01 SKU01:2 SKU02:1");
                return;
            }
            String customerId = tokens.get(0);
            Map<String, Integer> items = new LinkedHashMap<>();
            for (int i = 1; i < tokens.size(); i++) {
                String[] kv = tokens.get(i).split(":");
                if (kv.length != 2)
                    throw new IllegalArgumentException("Item invalido (use sku:qtd): " + tokens.get(i));
                items.merge(kv[0], Integer.parseInt(kv[1]), Integer::sum);
            }
            Order order = orderService.createOrder(customerId, items);
            System.out.println("[OK] Pedido criado: " + order);
            System.out.println("  Subtotal: R$ " + order.getSubtotal());
        }

        private void handleReserve(String args) {
            String orderId = args.trim();
            if (orderId.isEmpty()) { System.out.println("Uso: reserve <orderId>"); return; }
            Order order = orderService.reserveStock(orderId);
            System.out.println("[OK] Estoque reservado. " + order);
        }

        private void handleDiscount(String args) {
            List<String> tokens = parseTokens(args);
            if (tokens.size() != 2) { System.out.println("Uso: discount <orderId> <valor>"); return; }
            Order order = orderService.applyDiscount(tokens.get(0), new BigDecimal(tokens.get(1)));
            System.out.println("[OK] Desconto aplicado. Total: R$ " + order.getTotal());
        }

        private void handlePay(String args) {
            String orderId = args.trim();
            if (orderId.isEmpty()) { System.out.println("Uso: pay <orderId>"); return; }
            Order order = orderService.payOrder(orderId);
            String prefix = order.getStatus() == OrderStatus.PAID ? "[OK]" : "[FAIL]";
            System.out.println(prefix + " Pagamento processado: " + order.getStatus()
                + " | Total: R$ " + order.getTotal());
        }

        private void handleCancel(String args) {
            String orderId = args.trim();
            if (orderId.isEmpty()) { System.out.println("Uso: cancel <orderId>"); return; }
            Order order = orderService.cancelOrder(orderId);
            System.out.println("[OK] Pedido cancelado: " + order);
        }

        private void handleListOrders() {
            List<Order> orders = orderService.listAll();
            if (orders.isEmpty()) { System.out.println("(nenhum pedido)"); return; }
            System.out.println("Pedidos:");
            orders.forEach(o -> System.out.println("  " + o));
        }

        private void handleReport(String args) {
            String which = args.trim().toLowerCase();
            switch (which) {
                case "revenue" -> System.out.printf("Faturamento total (PAID): R$ %.2f%n",
                    reportService.totalRevenuePaid());

                case "top3" -> {
                    List<ReportService.ProductSales> top = reportService.top3BestSellers();
                    if (top.isEmpty()) { System.out.println("(sem vendas pagas ainda)"); return; }
                    System.out.println("Top 3 produtos mais vendidos:");
                    int rank = 1;
                    for (ReportService.ProductSales ps : top) {
                        System.out.printf("  %d. [%s] %s - %d unidades%n",
                            rank++, ps.sku(), ps.name(), ps.quantitySold());
                    }
                }

                case "category" -> {
                    Map<String, BigDecimal> byCat = reportService.revenueByCategory();
                    if (byCat.isEmpty()) { System.out.println("(sem vendas pagas ainda)"); return; }
                    System.out.println("Faturamento por categoria:");
                    byCat.forEach((cat, total) ->
                        System.out.printf("  %-20s R$ %.2f%n", cat, total));
                }

                case "customers" -> {
                    List<ReportService.CustomerOrderCount> ranking = reportService.customersByOrderCount();
                    if (ranking.isEmpty()) { System.out.println("(sem pedidos)"); return; }
                    System.out.println("Clientes por numero de pedidos:");
                    int rank = 1;
                    for (ReportService.CustomerOrderCount rc : ranking) {
                        System.out.printf("  %d. %s - %d pedido(s)%n",
                            rank++, rc.customerId(), rc.orderCount());
                    }
                }

                case "all", "" -> {
                    handleReport("revenue");
                    handleReport("top3");
                    handleReport("category");
                    handleReport("customers");
                }

                default -> System.out.println(
                    "Relatorios: report revenue | top3 | category | customers | all");
            }
        }

        private List<String> parseTokens(String input) {
            List<String> tokens = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            for (char c : input.toCharArray()) {
                if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (Character.isWhitespace(c) && !inQuotes) {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(c);
                }
            }
            if (current.length() > 0) tokens.add(current.toString());
            return tokens;
        }

        private void printBanner() {
            System.out.println("===========================================================");
            System.out.println("  SIMULADOR DE PROCESSAMENTO DE PEDIDOS - E-COMMERCE CLI");
            System.out.println("  Atividade 10  |  Java 21");
            System.out.println("===========================================================");
        }

        private void printHelp() {
            System.out.println();
            System.out.println("COMANDOS DISPONIVEIS:");
            System.out.println();
            System.out.println("Produtos:");
            System.out.println("  add-product <sku> <\"nome\"> <categoria> <preco> <estoque>");
            System.out.println("  list-products [sku|price]      ordena por sku (padrao) ou preco");
            System.out.println();
            System.out.println("Clientes:");
            System.out.println("  add-customer <id> <\"nome\"> <email>");
            System.out.println("  list-customers");
            System.out.println();
            System.out.println("Pedidos:");
            System.out.println("  create-order <customerId> <sku1:qtd1> [sku2:qtd2 ...]");
            System.out.println("  reserve <orderId>              reserva estoque (-> RESERVED)");
            System.out.println("  discount <orderId> <valor>     aplica desconto");
            System.out.println("  pay <orderId>                  processa pagamento (-> PAID/FAILED)");
            System.out.println("  cancel <orderId>               cancela e libera estoque");
            System.out.println("  list-orders");
            System.out.println();
            System.out.println("Relatorios:");
            System.out.println("  report revenue       faturamento total (PAID)");
            System.out.println("  report top3          top 3 produtos por quantidade");
            System.out.println("  report category      faturamento por categoria");
            System.out.println("  report customers     clientes por numero de pedidos");
            System.out.println("  report all           todos acima");
            System.out.println();
            System.out.println("Utilitarios:");
            System.out.println("  demo                 carrega dados de demonstracao");
            System.out.println("  help                 mostra esta ajuda");
            System.out.println("  exit                 encerra o sistema");
        }

        private void loadDemoData() {
            try {
                productService.addProduct("SKU01", "Notebook Dell", "eletronicos", new BigDecimal("3500.00"), 10);
                productService.addProduct("SKU02", "Mouse Logitech", "eletronicos", new BigDecimal("120.00"), 50);
                productService.addProduct("SKU03", "Livro Java", "livros", new BigDecimal("89.90"), 20);
                productService.addProduct("SKU04", "Cafeteira", "eletrodomesticos", new BigDecimal("250.00"), 15);

                customerService.addCustomer("C01", "Maria Silva", "maria@email.com");
                customerService.addCustomer("C02", "Joao Souza", "joao@email.com");
                customerService.addCustomer("C03", "Ana Costa", "ana@email.com");

                System.out.println("[OK] Dados demo carregados: 4 produtos, 3 clientes.");
                System.out.println("  Experimente: create-order C01 SKU01:1 SKU02:2");
            } catch (IllegalArgumentException e) {
                System.out.println("(dados demo ja carregados)");
            }
        }
    }
}