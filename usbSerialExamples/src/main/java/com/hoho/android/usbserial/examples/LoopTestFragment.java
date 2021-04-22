package com.hoho.android.usbserial.examples;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.forth.vm.vmc.api.Payter;
import com.forth.vm.vmc.api.VMC;
import com.forth.vm.vmc.model.Status;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoopTestFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoopTestFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private Button btnStart;
    private Button btnStop;

    int round = 0;
    int itemNo = 0;
    int roundAmount = 1;
    int releaseTimeout = 8000;
    int counterMilli = 200;
    int timeOut;

    //private BroadcastReceiver broadcastReceiver;
    private TextView receiveText;

    public static VMC vmc = new VMC();;
    public static Payter payter = new Payter();
    private boolean closed;
    private String lastPayterMsg = "";
    private String lastVMCMsg = "";

    private View view;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public LoopTestFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LoopTestFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LoopTestFragment newInstance(String param1, String param2) {
        LoopTestFragment fragment = new LoopTestFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_loop_test, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        btnStart = view.findViewById(R.id.btn_start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closed = false;
                round = 0;
                receiveText.setText("Start");
                EditText editText = getView().findViewById(R.id.et_round_amount);
                String temp = editText.getText().toString();
                if (!"".equals(temp)){
                    roundAmount = Integer.parseInt(temp);
                }
                Context context = getContext();
                payter.open(context);
                vmc.open(context);
                //status(vmc.getMessage());

                new Thread() {
                    public void run() {
                        for(; ++round<= roundAmount;) {
                            if(closed)
                                break;
                            if((round % 10) == 0)
                                receiveText.setText("");
                            itemNo = 0;
                            for(; ++itemNo<=10;) {
                                if(closed)
                                    break;
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        status("\nRound " + round + " : Slot " + itemNo);
                                    }
                                });
                                payter.vendRequest(2.00 * itemNo, itemNo);
                                timeOut = releaseTimeout;
                                while(timeOut > 0) {
                                    if(closed)
                                        break;
                                    try {
                                        Thread.sleep(counterMilli);
                                        timeOut -= counterMilli;

                                        if(payter.getStage() == Status.VEND_SUCCESS) {
                                            if(vmc.getStage() == Status.READY) {
                                                //status("  Payter: Payment success");
                                                //status("     VMC: Release item# " + itemNo);
                                                vmc.releaseItem(itemNo, 5000);
                                            }
                                        }
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if(payter.getStage() == Status.VEND_SUCCESS) {
                                                    String msg = "     Payter: Payment success";
                                                    if(!lastPayterMsg.equals(msg)) {
                                                        status(msg);
                                                        lastPayterMsg = msg;
                                                    }
                                                }
                                                if(vmc.getStage() == Status.RELEASE_ITEM) {
                                                    String msg = "     VMC: Release item# " + itemNo;
                                                    if(!lastVMCMsg.equals(msg)) {
                                                        status(msg);
                                                        lastVMCMsg = msg;
                                                    }
                                                }
                                                if(vmc.getStage() == Status.ITEM_DROP_TO_IR) {
                                                    String msg = "     VMC: Item drop to IR";
                                                    if(!lastVMCMsg.equals(msg)) {
                                                        //status("     VMC: " + msg);
                                                        lastVMCMsg = msg;
                                                    }
                                                }
                                                if(vmc.getStage() == Status.ITEM_PASS_IR) {
                                                    String msg = "     VMC: Item dropped";
                                                    if(!lastVMCMsg.equals(msg)) {
                                                        //status("     VMC: " + msg);
                                                        lastVMCMsg = msg;
                                                    }
                                                }
                                            }
                                        });
                                        if(vmc.getStage() == Status.ITEM_PASS_IR) {
                                            //status("     VMC: Item dropped");
                                            break;
                                        }
                                    }
                                    catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if(payter.getStage() != Status.VEND_SUCCESS) {
                                    //status("  Payter: Payment failed");
                                }
                                payter.vendEnd();
                                vmc.setStage(Status.READY);
                            }
                        }

                        //status(" - Finish - ");
                        vmc.close();
                        payter.close();
                    }
                }.start();
            }

        });

        btnStop = view.findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                closed = true;
            }
        });

        return view;
    }

    void status(String str) {
        if(receiveText == null || str == null)
            return;
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }
}