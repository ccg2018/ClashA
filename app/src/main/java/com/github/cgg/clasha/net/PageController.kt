package com.github.cgg.clasha.net

import com.yanzhenjie.andserver.annotation.Controller
import com.yanzhenjie.andserver.annotation.GetMapping

/**
 * @Author: ccg
 * @Email: ccgccg2019@gmail.com
 * @program: ClashA
 * @create: 2019-09-19
 * @describe
 */
@Controller
class PageController {

    @GetMapping(path = ["/"])
    fun index(): String {
        return "redirect:/index.html"
    }
}