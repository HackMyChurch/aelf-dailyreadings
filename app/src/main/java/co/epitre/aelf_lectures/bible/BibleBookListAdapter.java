package co.epitre.aelf_lectures.bible;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;

import co.epitre.aelf_lectures.R;
import co.epitre.aelf_lectures.bible.data.BibleBookEntry;
import co.epitre.aelf_lectures.bible.data.BibleBookEntryType;
import co.epitre.aelf_lectures.bible.data.BibleBookList;
import co.epitre.aelf_lectures.bible.data.BiblePart;

public class BibleBookListAdapter extends RecyclerView.Adapter<BibleBookListAdapter.ViewHolder> {

    /**
     * Event listeners
     */

    public class OnBibleEntryClickEvent {
        public final int mBiblePartId;
        public final int mBibleBookId;

        public OnBibleEntryClickEvent(int biblePartId, int bibleBookId) {
            this.mBiblePartId = biblePartId;
            this.mBibleBookId = bibleBookId;
        }
    }

    /**
     * Book list
     */
    private int mBiblePartId;
    private BiblePart mBiblePart;

    BibleBookListAdapter(int biblePartId) {
        mBiblePartId = biblePartId;
        mBiblePart = BibleBookList.getInstance().getParts().get(mBiblePartId);
    }

    @Override
    public int getItemViewType(int position) {
        BibleBookEntry bookEntry = mBiblePart.getBibleBookEntries().get(position);
        return bookEntry.getType().getValue();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Decode item type
        BibleBookEntryType bookEntryType = BibleBookEntryType.fromValue(viewType);

        // Create a view holder of this type
        int layoutId = R.layout.item_bible_book_name;
        switch (bookEntryType) {
            case SECTION:
                layoutId = R.layout.item_bible_section_name;
                break;
            case BOOK:
                layoutId = R.layout.item_bible_book_name;
                break;
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(v, bookEntryType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BibleBookEntry bookEntry = mBiblePart.getBibleBookEntries().get(position);

        // Set the Title
        TextView textView = holder.itemView.findViewById(R.id.title);
        textView.setText(bookEntry.getEntryName());

        // Register click data
        holder.mPostition = position;
    }

    @Override
    public int getItemCount() {
        return mBiblePart.getBibleBookEntries().size();
    }

    // When an item is a title, the grid layout will give it a ful row
    boolean isTitle(int position) {
        if (position >= getItemCount()) {
            return false;
        }

        return mBiblePart.getBibleBookEntries().get(position).getType() == BibleBookEntryType.SECTION;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        int mPostition = -1;

        ViewHolder(@NonNull View itemView, BibleBookEntryType bookEntryType) {
            super(itemView);
            if (bookEntryType == BibleBookEntryType.BOOK) {
                itemView.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            // Forward event
            EventBus.getDefault().post(new OnBibleEntryClickEvent(mBiblePartId, mPostition));
        }
    }
}
