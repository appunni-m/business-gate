package io.github.appunnim.businessgate.measure;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Isolated synthetic surface for checking this developer tool itself. */
public final class FixtureActivity extends Activity {
    private static volatile java.lang.ref.WeakReference<FixtureActivity> current = new java.lang.ref.WeakReference<>(null);
    static FixtureActivity active() { return current.get(); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this); root.setId(R.id.fixture_root); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 128, 32, 32);
        TextView text = new TextView(this); text.setId(R.id.fixture_label); text.setText(R.string.fixture);
        Button button = new Button(this); button.setId(R.id.fixture_control); button.setText(R.string.fixture_control);
        root.addView(text); root.addView(button); setContentView(root); current = new java.lang.ref.WeakReference<>(this);
    }
    @Override public void onDestroy() { if (active() == this) current.clear(); super.onDestroy(); }
}
