package utilities;

public class GenerateHashes {
    public static void main(String[] args) {
        String adminHash    = PasswordUtiities.hashPassword("admin123");
        String vendedorHash = PasswordUtiities.hashPassword("vendedor123");

        System.out.println("admin123    -> " + adminHash);
        System.out.println("vendedor123 -> " + vendedorHash);
    }
}