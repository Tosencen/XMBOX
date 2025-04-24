package com.github.tvbox.osc.bean

data class DoubanSuggestBean(
    var id: String,
    var episode: String,
    var img: String,
    var title: String,
    var url: String,
    var type: String,
    var year: String,
){
    var doubanRating: String? = null
        get() {
            return field?:""
        }
    var imdbRating: String? = null
        get() {
            return field?:""
        }
    var rottenRating: String? = null
        get() {
            return field?:""
        }
}
