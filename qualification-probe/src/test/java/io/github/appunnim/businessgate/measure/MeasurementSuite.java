package io.github.appunnim.businessgate.measure;

import java.util.ArrayList;
import java.util.List;

public final class MeasurementSuite {
    private static int assertions;
    private static void check(boolean value) { assertions++; if (!value) throw new AssertionError("measurement check " + assertions); }
    private static void fails(Runnable work) { try { work.run(); } catch (IllegalArgumentException | IllegalStateException expected) { assertions++; return; } throw new AssertionError("missing rejection"); }
    private static final class Node {
        final List<Node> children = new ArrayList<>();
        boolean selected = true;
        int released;
    }
    private static final class Access implements ExactPath.Access<Node> {
        final List<Integer> visited = new ArrayList<>();
        int reads;
        @Override public boolean belongs(Node node) { return node.selected; }
        @Override public int children(Node node) { return node.children.size(); }
        @Override public Node child(Node node, int index) { visited.add(index); return node.children.get(index); }
        @Override public void release(Node node) { node.released++; }
    }
    public static void main(String[] args) {
        check(ExactPath.parse("").isEmpty());
        check(ExactPath.parse("0,63,2").equals(List.of(0,63,2)));
        for (String invalid : new String[]{"-1", "64", "01", ",", "1,", "a", "1 2", "0,0,0,0,0,0,0,0,0,0,0,0,0"}) fails(() -> ExactPath.parse(invalid));
        fails(() -> ExactPath.parse(null));
        Node root = new Node(), forbidden = new Node(), chosen = new Node(), leaf = new Node();
        root.children.add(forbidden); root.children.add(chosen); chosen.children.add(leaf);
        Access access = new Access();
        int size = ExactPath.read(root, List.of(1,0), access, chain -> { access.reads++; return chain.size(); });
        check(size == 3); check(access.visited.equals(List.of(1,0))); check(access.reads == 1);
        check(root.released == 1 && chosen.released == 1 && leaf.released == 1 && forbidden.released == 0);
        Node foreign = new Node(), head = new Node(); foreign.selected = false; head.children.add(foreign);
        Access scoped = new Access();
        fails(() -> ExactPath.read(head, List.of(0), scoped, chain -> { throw new AssertionError("foreign capture"); }));
        check(head.released == 1 && foreign.released == 1);
        fails(() -> ExactPath.read(null, List.of(), new Access(), chain -> 1));
        Node tooWide = new Node(); for (int i=0;i<65;i++) tooWide.children.add(new Node());
        Access bounded = new Access();
        fails(() -> ExactPath.read(tooWide, List.of(0), bounded, chain -> 1)); check(bounded.visited.isEmpty()); check(tooWide.released == 1);
        Node broken = new Node(); broken.children.add(null);
        fails(() -> ExactPath.read(broken, List.of(0), new Access(), chain -> 1)); check(broken.released == 1);
        Node cycle = new Node(); cycle.children.add(cycle);
        fails(() -> ExactPath.read(cycle, List.of(0), new Access(), chain -> 1)); check(cycle.released == 1);
        Node throwing = new Node();
        fails(() -> ExactPath.read(throwing, List.of(), new Access(), chain -> { throw new IllegalStateException("capture"); })); check(throwing.released == 1);
        check(Neutral.safeIdentifier("android.widget.Button")); check(Neutral.safeIdentifier("profile_action"));
        check(!Neutral.safeIdentifier("path/value")); check(!Neutral.safeIdentifier("private value")); check(!Neutral.safeIdentifier(null));
        for (String number : new String[]{"+12025550100", "+1 (202) 555-0100", "+12025550123"}) check(Neutral.internationalPhoneSyntax(number));
        for (String number : new String[]{"12025550100", "+01234567", "+123", "+1234567890123456", "+12025550100 ext 9", "+1\u200e2025550100", "+12025550100\n"}) check(!Neutral.internationalPhoneSyntax(number));
        check(!Neutral.internationalPhoneSyntax(null));
        for (String label : new String[]{"Settings X", "Profile", "Ayarlar", "Edit profile", "A-B", "Full width Ｓｅｔｔｉｎｇｓ"}) check(Neutral.safeNavigationLabel(label));
        for (String label : new String[]{"", "1234567890", "+1 202 555 0100", "owner@example.test", "some.package", "Settings\nProfile", "A".repeat(41)}) check(!Neutral.safeNavigationLabel(label));
        check(!Neutral.safeNavigationLabel(null));
        check(Neutral.digest("abc").equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"));
        System.out.println("PASS " + assertions + " bounded measurement checks");
    }
}
