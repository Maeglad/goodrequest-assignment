package com.goodrequest.hiring.ui

import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.goodrequest.hiring.R
import com.goodrequest.hiring.databinding.ItemBinding
import com.goodrequest.hiring.databinding.LastItemBinding

class PokemonAdapter(
    private val onRetry: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = ArrayList<Pokemon>()
    private var isError = false

    companion object {
        private const val VIEW_TYPE_POKEMON = 0
        private const val VIEW_TYPE_LAST = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == VIEW_TYPE_POKEMON) {
            Item(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))
        } else {
            LastItem(LayoutInflater.from(parent.context).inflate(R.layout.last_item, parent, false),
                onRetry = onRetry)
        }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        if (holder.itemViewType == 0) {
            (holder as Item).show(items[position])
        } else {
            (holder as LastItem).show(isError)
        }


    override fun getItemViewType(position: Int): Int {
        return if (position < items.size) VIEW_TYPE_POKEMON
        else VIEW_TYPE_LAST
    }

    override fun getItemCount(): Int =
        items.size + 1

    fun show(pokemons: List<Pokemon>) {
        // calculate which items are different
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int =
                items.size

            override fun getNewListSize(): Int =
                pokemons.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                items[oldItemPosition].id == pokemons[newItemPosition].id

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                items[oldItemPosition] == pokemons[newItemPosition]

        })
        items.clear()
        items.addAll(pokemons)
        // update changed items
        diffResult.dispatchUpdatesTo(this)
    }

    fun setPagingError(isError: Boolean) {
        this.isError = isError
        notifyItemChanged(itemCount - 1)
    }
}

class Item(view: View) : RecyclerView.ViewHolder(view) {
    private val ui = ItemBinding.bind(view)

    fun show(pokemon: Pokemon) {
        ui.image.load(pokemon.detail?.image) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_foreground)
            fallback(R.drawable.ic_launcher_foreground)
        }
        ui.name.text = pokemon.name
        ui.move.text = pokemon.detail?.move?.replace("-", " ")
        ui.weight.text = pokemon.detail?.weight?.let { "$it kg" }
    }
}

class LastItem(view: View, onRetry: () -> Unit) : RecyclerView.ViewHolder(view) {
    private val ui = LastItemBinding.bind(view)

    init {
        ui.retry.setOnClickListener { onRetry() }
    }

    fun show(isError: Boolean) {
        if (isError) {
            ui.loading.visibility = INVISIBLE
            ui.retry.visibility = VISIBLE
        } else {
            ui.loading.visibility = VISIBLE
            ui.retry.visibility = INVISIBLE
        }
    }
}