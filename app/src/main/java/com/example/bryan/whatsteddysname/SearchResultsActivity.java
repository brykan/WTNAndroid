package com.example.bryan.whatsteddysname;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import static com.example.bryan.whatsteddysname.CollectionActivity.REQUEST_VIEW_ITEM;

public class SearchResultsActivity extends AppCompatActivity {
    private ListView resultsList;
    private List<String> results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        resultsList = (ListView) findViewById(R.id.resultsList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();

        results = (List<String>) intent.getStringArrayListExtra("results");

        ItemList adapter = new ItemList(this, results);
        resultsList.setAdapter(adapter);

        resultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent = new Intent(SearchResultsActivity.this, ItemActivity.class);

                intent.putExtra("ITEM", results.get(position));
                intent.putExtra("ITEMINDEX", position);
                startActivityForResult(intent, REQUEST_VIEW_ITEM);
                finish();
            }
        });
    }
}
