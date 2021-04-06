package piuk.blockchain.androidcore.utils.helperfunctions;

public class JavaHashCode {
    public static int hashCode(Long variable) {
        return Long.valueOf(variable).hashCode();
    }

    public static int hashCode(int variable) {
        return Long.valueOf(variable).hashCode();
    }

    public static int hashCode(boolean variable) {
        return Boolean.valueOf(variable).hashCode();
    }
}
