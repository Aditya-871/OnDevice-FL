package org.tensorflow.lite.examples.transfer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// defines the class to handle update messages during federated learning process
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    // list of messages
    private List<String> messages;

    // Constructor to initialize the data source
    public MessageAdapter(List<String> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        String message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    public void setData(List<String> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }


    // ViewHolder class
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }

        // Bind data to the TextView
        public void bind(String message) {
            messageTextView.setText(message);
        }
    }
}
