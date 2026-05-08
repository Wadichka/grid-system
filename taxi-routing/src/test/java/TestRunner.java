import com.gridcomputing.taxi_routing.TaxiSolver;

public class TestRunner {

    public static void main(String[] args) {
        TaxiSolver solver = new TaxiSolver();

        String shared = """
            4
            0 1 100 -1
            1 0 -1 100
            100 -1 0 1
            -1 100 1 0
            2
            0 3
            2
            1 2
            0 0
        """;

        // Префикс пустой — полный перебор
        String subtaskNoPrefix = "0\n";
        byte[] resultFull = solver.solve(shared.getBytes(), subtaskNoPrefix.getBytes());
        System.out.println("=== Полный перебор ===");
        System.out.println(new String(resultFull));

        // Префикс: фиксируем taxi[0] → passenger[0]
        String subtaskFix0 = "1\n0 0\n";
        byte[] result0 = solver.solve(shared.getBytes(), subtaskFix0.getBytes());
        System.out.println("=== Префикс: taxi[0]→passenger[0] ===");
        System.out.println(new String(result0));

        // Префикс: фиксируем taxi[0] → passenger[1]
        String subtaskFix1 = "1\n0 1\n";
        byte[] result1 = solver.solve(shared.getBytes(), subtaskFix1.getBytes());
        System.out.println("=== Префикс: taxi[0]→passenger[1] ===");
        System.out.println(new String(result1));
    }
}