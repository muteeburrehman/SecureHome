package io.xconn.securehome.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.xconn.securehome.R;
import java.util.ArrayList;
import java.util.List;

public class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder> {
    private final List<Uri> imageUris;
    private final Context context;
    private final OnImageRemoveListener listener;

    public interface OnImageRemoveListener {
        void onImageRemove(int position);
    }

    public SelectedImagesAdapter(Context context, OnImageRemoveListener listener) {
        this.context = context;
        this.imageUris = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selected_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);
        holder.imageView.setImageURI(imageUri);
        holder.removeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageRemove(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    // New method to add multiple images
    public void addImages(List<Uri> uris) {
        if (uris != null && !uris.isEmpty()) {
            int startPosition = imageUris.size();
            imageUris.addAll(uris);
            notifyItemRangeInserted(startPosition, uris.size());
        }
    }

    public void removeImage(int position) {
        if (position >= 0 && position < imageUris.size()) {
            imageUris.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, imageUris.size());
        }
    }

    public List<Uri> getImageUris() {
        return new ArrayList<>(imageUris);
    }

    // Make ImageViewHolder public to resolve the visibility issue
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView removeButton;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.selected_image);
            removeButton = itemView.findViewById(R.id.btn_remove_image);
        }
    }
}
