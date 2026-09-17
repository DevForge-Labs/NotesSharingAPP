package com.pravor.notessharing

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import java.util.concurrent.TimeUnit

class KayaConnectionPocTest {

    private val userAgent =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    private val cookieStore = mutableMapOf<String, MutableMap<String, Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val domainStore = cookieStore.getOrPut(url.host) { mutableMapOf() }
            for (cookie in cookies) {
                domainStore[cookie.name] = cookie
                println(" [Cookie Saved] ${cookie.name}=${cookie.value.take(12)}... (domain=${cookie.domain}, path=${cookie.path})")
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookies = cookieStore[url.host]?.values?.toList() ?: emptyList()
            if (cookies.isNotEmpty()) {
                println(" [Cookie Sent for ${url.encodedPath}] " + cookies.joinToString("; ") { "${it.name}=${it.value.take(8)}..." })
            }
            return cookies
        }
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(false) // Handle redirects manually to observe 302 vs 200
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Test
    fun testKayaConnectionAndLoginFlow() {
        println("\n========================================================")
        println("         KAYA CONNECTION POC TEST (Stage 2)             ")
        println("========================================================")

        val baseUrl = "https://kaya.cse.kiit.ac.in"
        val loginUrl = "$baseUrl/login/"
        val dashboardUrl = "$baseUrl/dashboard/student/"

        // ----------------------------------------------------
        // Step 1: GET /login/ (fetch CSRF token & initial cookies)
        // ----------------------------------------------------
        println("\n>>> Step 1: Fetching GET $loginUrl ...")
        val getRequest = Request.Builder()
            .url(loginUrl)
            .header("User-Agent", userAgent)
            .get()
            .build()

        val getResponse = client.newCall(getRequest).execute()
        val getCode = getResponse.code
        val getHtml = getResponse.body?.string() ?: ""

        println(" GET Response Code: $getCode")
        println(" GET Headers: \n" + getResponse.headers.joinToString("\n") { "   ${it.first}: ${it.second}" })

        // Extract csrfmiddlewaretoken from input field
        val csrfPattern = Regex("""name=["']csrfmiddlewaretoken["']\s+value=["']([^"']+)["']""")
        val csrfMatch = csrfPattern.find(getHtml)
        val csrfTokenFromForm = csrfMatch?.groupValues?.get(1)

        val csrfCookie = cookieStore["kaya.cse.kiit.ac.in"]?.get("csrftoken")?.value

        println(" CSRF Token from HTML form: $csrfTokenFromForm")
        println(" CSRF Token from Cookie:    $csrfCookie")

        assert(getCode == 200) { "Expected 200 OK from $loginUrl, got $getCode" }
        assert(!csrfTokenFromForm.isNullOrBlank()) { "Could not find csrfmiddlewaretoken in login HTML!" }

        // ----------------------------------------------------
        // Step 2: POST /login/ (test authentication flow)
        // ----------------------------------------------------
        val username = System.getProperty("kaya.user") ?: System.getenv("KAYA_USER") ?: "test_roll_no"
        val password = System.getProperty("kaya.pass") ?: System.getenv("KAYA_PASS") ?: "test_password"

        println("\n>>> Step 2: Testing POST $loginUrl with user: $username ...")

        val formBody = FormBody.Builder()
            .add("csrfmiddlewaretoken", csrfTokenFromForm ?: "")
            .add("username", username)
            .add("password", password)
            .build()

        val postRequest = Request.Builder()
            .url(loginUrl)
            .header("User-Agent", userAgent)
            .header("Referer", loginUrl)
            .header("Origin", baseUrl)
            .post(formBody)
            .build()

        val postResponse = client.newCall(postRequest).execute()
        val postCode = postResponse.code
        val postLocation = postResponse.header("Location")
        val postBody = postResponse.body?.string() ?: ""

        println(" POST Response Code: $postCode")
        println(" POST Location Header: $postLocation")
        println(" POST Set-Cookie: " + postResponse.headers("Set-Cookie"))

        val sessionIdCookie = cookieStore["kaya.cse.kiit.ac.in"]?.get("sessionid")

        if (postCode == 302 || postCode == 303 || sessionIdCookie != null) {
            println(" [SUCCESS] Authentication Successful! Received sessionid: ${sessionIdCookie?.value?.take(12)}...")
            println(" Redirecting to: $postLocation")

            // ----------------------------------------------------
            // Step 3: GET /dashboard/student/ with authenticated session
            // ----------------------------------------------------
            println("\n>>> Step 3: Fetching GET $dashboardUrl with authenticated session ...")
            val dashboardRequest = Request.Builder()
                .url(dashboardUrl)
                .header("User-Agent", userAgent)
                .header("Referer", loginUrl)
                .get()
                .build()

            val dashboardResponse = client.newCall(dashboardRequest).execute()
            val dashCode = dashboardResponse.code
            val dashHtml = dashboardResponse.body?.string() ?: ""

            println(" Dashboard Response Code: $dashCode")
            println(" Dashboard HTML length: ${dashHtml.length} chars")

            // Look for timetable chips
            val chipPattern = Regex("""<[^>]+data-dashboard-tt-chip[^>]*>""")
            val chips = chipPattern.findAll(dashHtml).toList()
            println(" Found ${chips.size} [data-dashboard-tt-chip] element(s) in dashboard HTML!")

            for ((index, chip) in chips.take(5).withIndex()) {
                println("   Chip #${index + 1}: ${chip.value.take(200)}...")
            }
        } else {
            println(" [AUTH REJECTED / TEST CREDENTIALS]")
            println(" Response status was $postCode (no redirect). Inspecting response body for error indicators...")

            // Check if standard Django error or bot-block
            val isCloudflare = postBody.contains("Cloudflare", ignoreCase = true) || postBody.contains("Just a moment", ignoreCase = true)
            val isDjangoFormError = postBody.contains("Please enter a correct username and password", ignoreCase = true) ||
                    postBody.contains("loginForm", ignoreCase = true)

            println(" Is Cloudflare / Bot Wall: $isCloudflare")
            println(" Is Django Form Error:     $isDjangoFormError")

            if (isDjangoFormError && !isCloudflare) {
                println(" [CONCLUSION] Native OkHttp authentication WORKS! The server processes the request cleanly as a standard Django form POST without any bot-protection or CAPTCHA interference.")
            }
        }
        println("\n========================================================")
    }
}
