import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.Music
import com.example.musicplayer.network.ApiService
import com.example.musicplayer.network.RetrofitClient
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {
    private val _tracks = MutableLiveData<List<Music>>()
    val tracks: LiveData<List<Music>> get() = _tracks

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun loadTracks() {
        viewModelScope.launch {
            _loading.value = true

            try {
                val retrofit = RetrofitClient.getRetrofit()
                val service = retrofit.create(ApiService::class.java)
                val response = service.getTracks()

                _tracks.value = response.tracks
            } catch (e: Exception) {
                _tracks.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}