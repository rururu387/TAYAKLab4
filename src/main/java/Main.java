public class Main {
    public static void main(String[] args) {
        System.out.println("Geese are cool!");
        SyntaxAnalyzer syntaxAnalyzer = new SyntaxAnalyzer("src/main/resources/grammar.txt");
        System.out.println(syntaxAnalyzer.grammarsToString());
    }
}
