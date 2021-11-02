public class Symbol {
    boolean isTerminal;
    String value;

    public String getValue() {
        return value;
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    public Symbol(boolean isTerminal, String value) {
        this.isTerminal = isTerminal;
        this.value = value;
    }

    public String toString() {
        if (!isTerminal) {
            return "<" + value + ">";
        }
        if (value.equals(""))
            return "#eps#";
        return value;
    }
}
