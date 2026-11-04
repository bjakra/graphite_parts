package org.graphite.parts.nowplaying

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import org.graphite.parts.R

/**
 * Ambient Music Mod's two country pickers: the database country (or automatic, from the phone's region) and up to
 * two extra countries. Graphite Now Playing applies them with AMM's logic. Their songs have to be downloaded, so after
 * a change the user is asked to update (which connects to Google), as on the music database page.
 */
object NowPlayingCountries {

    fun pickCountry(context: Context, countries: Bundle, onChanged: () -> Unit) {
        val codes = listOf("") + countries.getStringArray("codes").orEmpty()
        val names = listOf(context.getString(R.string.nowplaying_country_automatic)) +
            countries.getStringArray("names").orEmpty()
        val current = codes.indexOf(countries.getString("country").orEmpty()).coerceAtLeast(0)
        AlertDialog.Builder(context)
            .setTitle(R.string.nowplaying_country)
            .setSingleChoiceItems(names.toTypedArray(), current) { dialog, which ->
                dialog.dismiss()
                if (which == current) return@setSingleChoiceItems
                NowPlayingApi.setCountry(context, codes[which])
                onChanged()
                askToDownload(context)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun pickExtraCountries(context: Context, countries: Bundle, onChanged: () -> Unit) {
        showExtraCountries(context, countries, R.string.nowplaying_extra_countries,
            android.R.string.ok) {
            onChanged()
            askToDownload(context)
        }
    }

    /** First switch-on: asks which countries' songs to get, then downloads the database */
    fun setupCountries(context: Context, countries: Bundle) {
        showExtraCountries(context, countries, R.string.nowplaying_setup_countries,
            R.string.nowplaying_database_download) {
            NowPlayingApi.updateDatabase(context)
        }
    }

    private fun showExtraCountries(
        context: Context,
        countries: Bundle,
        title: Int,
        confirm: Int,
        onConfirmed: () -> Unit
    ) {
        val primary = countries.getString("primary")
        val codes = countries.getStringArray("codes").orEmpty().filter { it != primary }
        val allCodes = countries.getStringArray("codes").orEmpty()
        val allNames = countries.getStringArray("names").orEmpty()
        val names = codes.map { allNames[allCodes.indexOf(it)] }
        val chosen = countries.getStringArray("extras").orEmpty().toMutableSet()
        val max = countries.getInt("maxExtras", 2)
        val checked = BooleanArray(codes.size) { codes[it] in chosen }
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMultiChoiceItems(names.toTypedArray(), checked) { dialog, which, isChecked ->
                if (isChecked && chosen.size >= max) {
                    (dialog as AlertDialog).listView.setItemChecked(which, false)
                    Toast.makeText(context, context.getString(R.string.nowplaying_extra_countries_limit, max),
                        Toast.LENGTH_SHORT).show()
                    return@setMultiChoiceItems
                }
                if (isChecked) chosen += codes[which] else chosen -= codes[which]
            }
            .setPositiveButton(confirm) { _, _ ->
                NowPlayingApi.setExtraCountries(context, chosen.toTypedArray())
                onConfirmed()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun askToDownload(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(R.string.nowplaying_database_warning_title)
            .setMessage(R.string.nowplaying_country_download)
            .setPositiveButton(R.string.nowplaying_database_update) { _, _ -> NowPlayingApi.updateDatabase(context) }
            .setNegativeButton(R.string.nowplaying_country_later, null)
            .show()
    }
}
