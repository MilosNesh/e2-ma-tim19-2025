package com.example.habitgame.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.habitgame.R;
import com.example.habitgame.model.Account;
import com.example.habitgame.model.AccountCallback;
import com.example.habitgame.model.Category;
import com.example.habitgame.model.Task;
import com.example.habitgame.model.TaskStatus;
import com.example.habitgame.services.AccountService;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habitgame.services.CategoryService;
import com.example.habitgame.services.TaskService;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProgressFragment extends Fragment {

    private String myEmail;
    private AccountService accountService;

    private TextView titleText, ppText, xpText, maxXpText, activeDaysText;
    private ImageView titleImage;
    private ProgressBar progressBar;

    private LineChart lineChartActiveDays, lineChartXP;
    private PieChart pieChartTasks;
    private BarChart barChartCategory, barChartSpecialMissions;

    public ProgressFragment() {
    }
    public static ProgressFragment newInstance(String param1, String param2) {
        ProgressFragment fragment = new ProgressFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);
        titleText = view.findViewById(R.id.titleText);
        ppText = view.findViewById(R.id.ppText);
        xpText = view.findViewById(R.id.xpText);
        maxXpText = view.findViewById(R.id.maxXpText);
        progressBar = view.findViewById(R.id.xpProgressBar);
        titleImage = view.findViewById(R.id.titleImage);
//        lineChartActiveDays = view.findViewById(R.id.lineChartActiveDays);
        pieChartTasks = view.findViewById(R.id.pieChartTasks);
        barChartCategory = view.findViewById(R.id.barChartCategory);
        lineChartXP = view.findViewById(R.id.lineChartXP);
        barChartSpecialMissions = view.findViewById(R.id.barChartSpecialMissions);
        activeDaysText = view.findViewById(R.id.activeDays);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("HabitGamePrefs", getContext().MODE_PRIVATE);
        myEmail = sharedPreferences.getString("email", "");
        accountService = new AccountService();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        accountService.getAccountByEmail(myEmail, new AccountCallback() {
            @Override
            public void onResult(Account account) {
                int resID = getResources().getIdentifier("level"+account.getLevel(), "drawable", getActivity().getPackageName());
                titleImage.setImageResource(resID);
                account.newTitle();
                titleText.setText(account.getTitle());
                ppText.setText("Power points: " +account.getPowerPoints());
                xpText.setText("Osvojeni XP: " + account.getExperiencePoints());
                maxXpText.setText("Potrebno: "+account.countMaxXp());
                progressBar.setProgress(account.getExperiencePoints());
                progressBar.setMax(account.countMaxXp());

                showActiveDays();
                showTaskStatistics();
                showTaskCategories();
                showXPProgress();
                showSpecialMissions();
            }
            @Override
            public void onFailure(Exception e) {
                Log.e("ProgressFragment", "Greška pri učitavanju Accounta za Progress: ", e);
                Toast.makeText(getContext(), "Greška pri učitavanju napretka: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showActiveDays() {
        SharedPreferences prefs = getContext().getSharedPreferences("user_activity", Context.MODE_PRIVATE);
        Set<String> activeDays = prefs.getStringSet("active_days", new HashSet<>());
        activeDaysText.setText("Broj dana aktivnog korišćenja aplikacije: "+activeDays.size());
    }

    private void showTaskStatistics() {

        TaskService taskService = new TaskService();
        taskService.getTasksForCurrentUser()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Task> taskList = task.getResult();
                        int uradjeni = 0, kreirani = 0, neuradjeni = 0, otkazani = 0, ukupno = 0;

                        for(Task t : taskList) {
                            ukupno++;
                            switch (t.getStatus()){
                                case KREIRAN: kreirani++; break;
                                case URADJEN: uradjeni++; break;
                                case PAUZIRAN: neuradjeni++; break;
                                case OTKAZAN: otkazani++; break;
                            }

                        }

                        ArrayList<PieEntry> entries = new ArrayList<>();
                        if(kreirani != 0)
                            entries.add(new PieEntry(kreirani, "Kreirani"));
                        if(uradjeni != 0)
                            entries.add(new PieEntry(uradjeni, "Uradjeni"));
                        if(neuradjeni != 0)
                            entries.add(new PieEntry(neuradjeni, "Neuradjeni"));
                        if(otkazani != 0)
                            entries.add(new PieEntry(otkazani, "Otkazani"));

                        PieDataSet dataSet = new PieDataSet(entries, "Statistika zadtaka");
                        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                        PieData data = new PieData(dataSet);
                        pieChartTasks.setData(data);
                        pieChartTasks.setHoleColor(android.graphics.Color.TRANSPARENT);
                        pieChartTasks.invalidate();
                    } else {
                        Log.e("TaskService", "Neuspješno dohvaćanje taskova", task.getException());
                    }
                });

    }

    private void showTaskCategories() {
        TaskService taskService = new TaskService();
        ArrayList<BarEntry> entries = new ArrayList<>();
        Map<String, Integer> finishedTasks = new HashMap<>();

        CategoryService.getMyCategories().addOnCompleteListener(categoryTask -> {
            if (categoryTask.isSuccessful() && categoryTask.getResult() != null) {
                List<Category> categoryList = categoryTask.getResult();

                final int totalCategories = categoryList.size();
                final int[] completedCount = {0};

                ArrayList<String> labels = new ArrayList<>();

                for (Category c : categoryList) {
                    taskService.getTasksForCurrentUser().addOnSuccessListener(tasks -> {
                        int uradjen = 0;
                        for (Task t : tasks) {
                            if (t.getCategoryId().equals(c.getId()) && t.getStatus().equals(TaskStatus.URADJEN))
                                uradjen++;
                        }
                        finishedTasks.put(c.getName(), uradjen);

                        completedCount[0]++;

                        // Kad su obradili sve kategorije, kreira se graf
                        if (completedCount[0] == totalCategories) {
                            int i = 0;
                            for (Map.Entry<String, Integer> entry : finishedTasks.entrySet()) {
                                entries.add(new BarEntry(i, entry.getValue()));
                                labels.add(entry.getKey());
                                i++;
                            }

                            BarDataSet dataSet = new BarDataSet(entries, "Zavrseni zadaci po Kategoriji");
                            BarData data = new BarData(dataSet);
                            barChartCategory.setData(data);

                            // Podesi X osu
                            XAxis xAxis = barChartCategory.getXAxis();
                            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                            xAxis.setGranularity(1f);
                            xAxis.setGranularityEnabled(true);
                            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                            barChartCategory.invalidate();
                        }
                    });
                }
            }
        });
    }

    private void showXPProgress() {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 50));  // Day 1 - 50 XP
        entries.add(new Entry(1, 70));  // Day 2 - 70 XP
        entries.add(new Entry(2, 90));  // Day 3 - 90 XP
        entries.add(new Entry(3, 110)); // Day 4 - 110 XP
        entries.add(new Entry(4, 130)); // Day 5 - 130 XP

        LineDataSet dataSet = new LineDataSet(entries, "XP Progress");
        LineData lineData = new LineData(dataSet);
        lineChartXP.setData(lineData);
        lineChartXP.invalidate();
    }

    private void showSpecialMissions() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 5));
        entries.add(new BarEntry(1, 3));

        BarDataSet dataSet = new BarDataSet(entries, "Specialne misije");
        BarData data = new BarData(dataSet);
        barChartSpecialMissions.setData(data);

        ArrayList<String> labels = new ArrayList<>();
        labels.add("Zapocete");
        labels.add("Zavrsene");

        XAxis xAxis = barChartSpecialMissions.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        barChartSpecialMissions.invalidate();

    }
}