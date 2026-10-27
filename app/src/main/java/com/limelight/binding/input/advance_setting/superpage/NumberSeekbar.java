package com.limelight.binding.input.advance_setting.superpage;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.limelight.R;

public class NumberSeekbar extends LinearLayout {

    private TextView numberSeekbarTitle;
    private View numberSeekbarMinus;
    private TextView numberSeekbarNumber;
    private View numberSeekbarAdd;
    private SeekBar numberSeekbarSeekbar;
    private EditText numberSeekbarInput;
    private OnNumberSeekbarChangeListener onNumberSeekbarChangeListener;

    // 默认步进保持 1，保证未显式配置步进的设置行为不变
    private int step = 1;
    // 值变化是否正由输入框触发，用于避免回写输入框打断用户输入
    private boolean syncingInput = false;
    private boolean applyingFromInput = false;

    public interface OnNumberSeekbarChangeListener{
        void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser);
        void onStartTrackingTouch(SeekBar seekBar);
        void onStopTrackingTouch(SeekBar seekBar);
    }



    public NumberSeekbar(Context context) {
        super(context);
        init(context,null);
    }

    public NumberSeekbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context,attrs);
    }

    public NumberSeekbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }

    public NumberSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    private void init(Context context,AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.number_seekbar, this, true);

        numberSeekbarTitle = findViewById(R.id.number_seekbar_title);
        numberSeekbarMinus = findViewById(R.id.number_seekbar_minus);
        numberSeekbarNumber = findViewById(R.id.number_seekbar_number);
        numberSeekbarAdd = findViewById(R.id.number_seekbar_add);
        numberSeekbarSeekbar = findViewById(R.id.number_seekbar_seekbar);
        numberSeekbarInput = findViewById(R.id.number_seekbar_input);
        numberSeekbarNumber.setText(String.valueOf(numberSeekbarSeekbar.getProgress()));

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.NumberSeekbar,
                    0, 0);

            try {
                int maxValue = a.getInt(R.styleable.NumberSeekbar_max, 100);
                int minValue = a.getInt(R.styleable.NumberSeekbar_min, 0);
                int progressValue = a.getInt(R.styleable.NumberSeekbar_progress, 0);
                numberSeekbarSeekbar.setMax(maxValue);
                numberSeekbarSeekbar.setMin(minValue);
                numberSeekbarSeekbar.setProgress(progressValue);
                numberSeekbarNumber.setText(String.valueOf(progressValue));


                String title = a.getString(R.styleable.NumberSeekbar_text);
                numberSeekbarTitle.setText(title);

                // 输入框需显式开启，未开启的设置完全保持原有界面与行为
                boolean editable = a.getBoolean(R.styleable.NumberSeekbar_editable, false);
                numberSeekbarInput.setVisibility(editable ? VISIBLE : GONE);
                // 步进默认 1，未显式配置的设置 +/- 行为不变
                step = a.getInt(R.styleable.NumberSeekbar_step, 1);
                if (step < 1) {
                    step = 1;
                }
                if (editable) {
                    numberSeekbarNumber.setVisibility(GONE);
                    numberSeekbarInput.setText(String.valueOf(progressValue));
                }
            } finally {
                a.recycle();
            }
        }

        numberSeekbarInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (syncingInput) {
                    return;
                }
                Integer value = parseInput(String.valueOf(s));
                if (value == null) {
                    return;
                }
                applyInputValue(value);
            }
        });

        numberSeekbarInput.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    commitInput(true);
                    hideKeyboard(v);
                }
            }
        });

        numberSeekbarInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT) {
                    commitInput(true);
                    hideKeyboard(v);
                    v.clearFocus();
                    // 消费掉回车本身，避免这个按键被转发给串流主机
                    return true;
                }
                return false;
            }
        });

        numberSeekbarSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                numberSeekbarNumber.setText(String.valueOf(progress));
                syncInput(progress);
                if (onNumberSeekbarChangeListener != null){
                    onNumberSeekbarChangeListener.onProgressChanged(seekBar, progress, fromUser);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (onNumberSeekbarChangeListener != null){
                    onNumberSeekbarChangeListener.onStartTrackingTouch(seekBar);
                }

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (onNumberSeekbarChangeListener != null){
                    onNumberSeekbarChangeListener.onStopTrackingTouch(seekBar);
                }
            }
        });

        numberSeekbarMinus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int progress = numberSeekbarSeekbar.getProgress();
                if (progress > numberSeekbarSeekbar.getMin()) {
                    int target = Math.max(progress - step, numberSeekbarSeekbar.getMin());
                    setProgressByUser(target);
                }
            }
        });

        numberSeekbarAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int progress = numberSeekbarSeekbar.getProgress();
                if (progress < numberSeekbarSeekbar.getMax()) {
                    int target = Math.min(progress + step, numberSeekbarSeekbar.getMax());
                    setProgressByUser(target);
                }
            }
        });
    }

    // 由 +/- 按钮或滑杆等界面操作改变数值，需要同步刷新输入框
    private void setProgressByUser(int target) {
        if (onNumberSeekbarChangeListener != null){
            onNumberSeekbarChangeListener.onStartTrackingTouch(numberSeekbarSeekbar);
            numberSeekbarSeekbar.setProgress(target);
            onNumberSeekbarChangeListener.onStopTrackingTouch(numberSeekbarSeekbar);
        } else {
            numberSeekbarSeekbar.setProgress(target);
        }
        syncInput(target);
    }

    private static Integer parseInput(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void applyInputValue(int value) {
        int min = numberSeekbarSeekbar.getMin();
        int max = numberSeekbarSeekbar.getMax();
        int clamped = Math.max(min, Math.min(value, max));
        if (clamped != value) {
            // 超出范围时立即把输入框改回夹紧后的值
            syncInputText(String.valueOf(clamped));
        }
        if (numberSeekbarSeekbar.getProgress() == clamped) {
            return;
        }
        // 标记当前进度变化来自输入框，避免回写打断用户输入
        applyingFromInput = true;
        try {
            if (onNumberSeekbarChangeListener != null){
                onNumberSeekbarChangeListener.onStartTrackingTouch(numberSeekbarSeekbar);
                numberSeekbarSeekbar.setProgress(clamped);
                onNumberSeekbarChangeListener.onStopTrackingTouch(numberSeekbarSeekbar);
            } else {
                numberSeekbarSeekbar.setProgress(clamped);
            }
        } finally {
            applyingFromInput = false;
        }
    }

    private void commitInput(boolean force) {
        if (numberSeekbarInput.getVisibility() != VISIBLE) {
            return;
        }
        Integer value = parseInput(numberSeekbarInput.getText().toString());
        if (value == null) {
            // 输入非法（空或非数字）时回退到当前值
            syncInputText(String.valueOf(numberSeekbarSeekbar.getProgress()));
            return;
        }
        applyInputValue(value);
        if (force) {
            syncInputText(String.valueOf(numberSeekbarSeekbar.getProgress()));
        }
    }

    private void syncInput(int progress) {
        if (numberSeekbarInput == null || numberSeekbarInput.getVisibility() != VISIBLE) {
            return;
        }
        // 由输入框本身触发的进度变化不回写输入框，否则会打断用户输入
        if (applyingFromInput) {
            return;
        }
        syncInputText(String.valueOf(progress));
    }

    private void syncInputText(String text) {
        syncingInput = true;
        numberSeekbarInput.setText(text);
        numberSeekbarInput.setSelection(numberSeekbarInput.getText().length());
        syncingInput = false;
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public int getValue() {
        return numberSeekbarSeekbar.getProgress();
    }

    public void setValueWithNoCallBack(int value) {
        OnNumberSeekbarChangeListener onNumberSeekbarChangeListenerTemp = onNumberSeekbarChangeListener;
        onNumberSeekbarChangeListener = null;
        numberSeekbarSeekbar.setProgress(value);
        numberSeekbarNumber.setText(String.valueOf(value));
        syncInput(numberSeekbarSeekbar.getProgress());
        onNumberSeekbarChangeListener = onNumberSeekbarChangeListenerTemp;
    }

    public void setTitle(String title){
        numberSeekbarTitle.setText(title);
    }

    public void setProgressMax(int max){
        numberSeekbarSeekbar.setMax(max);
    }

    public void setProgressMin(int min){
        numberSeekbarSeekbar.setMin(min);
    }

    public void setOnNumberSeekbarChangeListener(OnNumberSeekbarChangeListener onNumberSeekbarChangeListener){
        this.onNumberSeekbarChangeListener = onNumberSeekbarChangeListener;
    }


}
