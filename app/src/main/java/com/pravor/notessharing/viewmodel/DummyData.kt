package com.pravor.notessharing.viewmodel

import com.pravor.notessharing.model.Category
import com.pravor.notessharing.model.Contributor
import com.pravor.notessharing.model.DiscoverFeedItem
import com.pravor.notessharing.model.FeedItem
import com.pravor.notessharing.model.FileType
import com.pravor.notessharing.model.Profile
import com.pravor.notessharing.model.RevisionCard
import com.pravor.notessharing.model.StudyFile
import com.pravor.notessharing.model.StudyCollection
import com.pravor.notessharing.model.TrendingNote
import com.pravor.notessharing.model.TrendingTopic
import com.pravor.notessharing.model.VideoRecommendation

internal object DummyData {
    val feedItems = listOf(
        FeedItem("feed-dbms-4", "Aarav Mehta", "AM", "May 22", "DBMS Unit 4 Complete Notes", "Normalization, transactions, indexing, and recovery notes cleaned up from class discussions.", listOf("DBMS", "CSE", "Unit 4"), FileType.Pdf, 128, 18, 642, false, false),
        FeedItem("feed-os-pyq", "Nisha Rao", "NR", "May 21", "Operating Systems PYQ Set 2021-2025", "Semester-wise solved questions with short hints for process scheduling and memory management.", listOf("OS", "PYQ", "Exam"), FileType.Pyq, 214, 32, 980, true, true),
        FeedItem("feed-java-lab", "Kabir Sinha", "KS", "May 20", "Java OOP Lab Manual Annotated", "Programs, viva prompts, and common compiler fixes for inheritance, interfaces, and exceptions.", listOf("Java", "Lab", "OOP"), FileType.LabManual, 91, 11, 354, false, true),
        FeedItem("feed-cn-cheat", "Meera Iyer", "MI", "May 18", "Computer Networks Cheat Sheet", "Subnetting, TCP flags, routing protocols, and OSI layers compressed into a quick revision sheet.", listOf("CN", "Cheat Sheet"), FileType.CheatSheet, 176, 24, 721, false, false),
        FeedItem("feed-math-guide", "Rohan Das", "RD", "May 17", "Discrete Maths Study Guide", "Graph theory, relations, recurrence, and proof techniques with exam-style examples.", listOf("Maths", "Guide", "CSE"), FileType.StudyGuide, 73, 9, 268, false, false),
        FeedItem("feed-coa-notes", "Sara Khan", "SK", "May 16", "COA Pipeline and Cache Notes", "Handwritten-to-digital notes covering hazards, cache mapping, and instruction cycles.", listOf("COA", "Notes"), FileType.Notes, 119, 15, 507, true, false),
        FeedItem("feed-ai-book", "Dev Patel", "DP", "May 15", "AI Reference Book Chapters", "Curated chapter references for search, logic, probability, and basic ML foundations.", listOf("AI", "Book", "Reference"), FileType.Book, 66, 6, 231, false, true),
        FeedItem("feed-web-video", "Ananya Sen", "AS", "May 14", "Web Tech Crash Course Playlist", "Topic-wise video notes for HTML, CSS, JavaScript, HTTP, and backend basics.", listOf("Web", "Videos"), FileType.Video, 143, 21, 612, false, false),
        FeedItem("feed-se-resources", "Vikram Joshi", "VJ", "May 13", "Software Engineering Semester Pack", "SRS formats, UML diagrams, testing notes, and project viva preparation in one pack.", listOf("SE", "Semester Pack"), FileType.StudyGuide, 188, 27, 844, false, true),
        FeedItem("feed-dsa-notes", "Ishita Bose", "IB", "May 12", "DSA Trees and Graphs Notes", "Traversal patterns, shortest paths, MST, and complexity notes with clean examples.", listOf("DSA", "Graphs", "Trees"), FileType.Notes, 251, 39, 1102, true, true),
        FeedItem("feed-dbms-lab", "Harsh Vardhan", "HV", "May 10", "SQL Lab Queries With Outputs", "DDL, DML, joins, nested queries, triggers, and stored procedures with screenshots.", listOf("SQL", "DBMS", "Lab"), FileType.LabManual, 97, 13, 389, false, false),
        FeedItem("feed-compiler-pyq", "Tanya Roy", "TR", "May 09", "Compiler Design Important PYQs", "Lexical analysis, parsing, intermediate code, and optimization questions sorted by frequency.", listOf("CD", "PYQ"), FileType.Pyq, 82, 7, 276, false, false),
        FeedItem("feed-cloud-guide", "Aditya Menon", "AM", "May 07", "Cloud Computing Unit 2 Guide", "Virtualization, containers, service models, and deployment models for quick exam revision.", listOf("Cloud", "Unit 2"), FileType.StudyGuide, 64, 5, 198, false, false),
        FeedItem("feed-ml-cheat", "Priya Nair", "PN", "May 05", "Machine Learning Formula Sheet", "Loss functions, metrics, gradient descent, regression, and classification formulas in one sheet.", listOf("ML", "Formula", "Cheat Sheet"), FileType.CheatSheet, 137, 17, 541, false, true)
    )

    val savedFiles = listOf(
        StudyFile("saved-os", "OS Deadlocks Quick Revision", "Saved May 19", FileType.Notes, 314, 88),
        StudyFile("saved-dbms-pyq", "DBMS Solved PYQs 2020-2025", "Saved May 18", FileType.Pyq, 842, 203),
        StudyFile("saved-java", "Java Collections Cheat Sheet", "Saved May 17", FileType.CheatSheet, 451, 134),
        StudyFile("saved-cn", "CN Routing Protocols Guide", "Saved May 16", FileType.StudyGuide, 290, 77),
        StudyFile("saved-dsa", "DSA Graph Algorithms Notes", "Saved May 15", FileType.Notes, 1102, 251),
        StudyFile("saved-ai", "AI Search Algorithms Booklet", "Saved May 14", FileType.Book, 221, 65),
        StudyFile("saved-web", "Web Tech Viva Questions", "Saved May 12", FileType.Pdf, 367, 99),
        StudyFile("saved-coa", "COA Cache Mapping Examples", "Saved May 11", FileType.Notes, 507, 119),
        StudyFile("saved-se", "SRS Template and Examples", "Saved May 09", FileType.Pdf, 188, 52),
        StudyFile("saved-cloud", "Cloud Service Models Guide", "Saved May 07", FileType.StudyGuide, 198, 64),
        StudyFile("saved-ml", "ML Metrics Cheat Sheet", "Saved May 05", FileType.CheatSheet, 541, 137)
    )

    val uploadedFiles = listOf(
        StudyFile("uploaded-java", "Java OOP Lab Manual", "Uploaded May 12", FileType.Pdf, 89, 24),
        StudyFile("uploaded-dbms", "ER Diagram Practice Set", "Uploaded May 10", FileType.Notes, 146, 41),
        StudyFile("uploaded-os", "CPU Scheduling Solved Examples", "Uploaded May 08", FileType.StudyGuide, 203, 58),
        StudyFile("uploaded-cn", "TCP/IP Revision Sheet", "Uploaded May 06", FileType.CheatSheet, 174, 46),
        StudyFile("uploaded-dsa", "Sorting Algorithms Lab Notes", "Uploaded May 04", FileType.LabManual, 267, 73),
        StudyFile("uploaded-web", "JavaScript DOM Practice", "Uploaded May 02", FileType.LabManual, 155, 32),
        StudyFile("uploaded-maths", "Recurrence Relations Notes", "Uploaded Apr 29", FileType.Notes, 121, 27),
        StudyFile("uploaded-se", "UML Diagram Examples", "Uploaded Apr 26", FileType.Pdf, 138, 35),
        StudyFile("uploaded-ai", "Minimax Algorithm Summary", "Uploaded Apr 23", FileType.StudyGuide, 96, 22),
        StudyFile("uploaded-coa", "Instruction Cycle Diagrams", "Uploaded Apr 20", FileType.Notes, 104, 19),
        StudyFile("uploaded-ml", "Regression Formula Sheet", "Uploaded Apr 18", FileType.CheatSheet, 118, 28)
    )



    val categories = Category.entries
    val topics = listOf(
        TrendingTopic("dbms", "DBMS", "Fresh notes"),
        TrendingTopic("os", "OS", "PYQs rising"),
        TrendingTopic("java", "Java", "Lab help"),
        TrendingTopic("cn", "CN", "Popular today"),
        TrendingTopic("dsa", "DSA", "Graphs week"),
        TrendingTopic("coa", "COA", "Cache focus"),
        TrendingTopic("ai", "AI", "Search notes"),
        TrendingTopic("web", "Web", "Viva prep"),
        TrendingTopic("se", "SE", "UML packs"),
        TrendingTopic("ml", "ML", "Formula saves"),
        TrendingTopic("cloud", "Cloud", "Unit guides")
    )

    val trendingNotes = listOf(
        "Operating Systems Complete Notes" to "OS",
        "DBMS Quick Revision" to "DBMS",
        "CN Exam Guide" to "CN",
        "Java Interview Notes" to "Java",
        "Data Structures Graph Pack" to "DSA",
        "Computer Architecture Cache Notes" to "COA",
        "Machine Learning Formula Sheet" to "ML",
        "AI Search Algorithms" to "AI",
        "Cloud Computing Unit 3" to "Cloud",
        "Cyber Security Basics" to "Security",
        "Python Placement Notes" to "Python",
        "Compiler Design Parser Guide" to "CD",
        "Software Engineering UML Notes" to "SE",
        "Discrete Maths Proof Guide" to "Maths",
        "Web Technology Viva Notes" to "Web",
        "SQL Joins Practice Notes" to "DBMS",
        "Deadlock Revision Sheet" to "OS",
        "TCP/IP Cheat Sheet" to "CN",
        "Java Collections Map" to "Java",
        "Big O Complexity Guide" to "DSA"
    ).mapIndexed { index, entry ->
        TrendingNote(
            id = "trending-note-$index",
            title = entry.first,
            subject = entry.second,
            downloads = 420 + index * 87,
            rating = 4.2 + (index % 8) * 0.1,
            upvotes = 64 + index * 13,
            isBookmarked = index % 5 == 0
        )
    }

    val videoRecommendations = listOf(
        Triple("Deadlock Explained", "Neso Academy", "OS"),
        Triple("DBMS in One Shot", "Gate Smashers", "DBMS"),
        Triple("Computer Networks Revision", "Knowledge Gate", "CN"),
        Triple("Java OOP Concepts", "Apna College", "Java"),
        Triple("Graph Algorithms Visualized", "Abdul Bari", "DSA"),
        Triple("SQL Joins Made Simple", "Jenny's Lectures", "DBMS"),
        Triple("Paging and Segmentation", "Neso Academy", "OS"),
        Triple("TCP vs UDP", "Gate Smashers", "CN"),
        Triple("Python Crash Course", "CodeWithHarry", "Python"),
        Triple("Machine Learning Basics", "StatQuest", "ML"),
        Triple("AI Search Strategies", "Knowledge Gate", "AI"),
        Triple("Cloud Computing Models", "Simplilearn", "Cloud"),
        Triple("Cyber Security Roadmap", "NetworkChuck", "Security"),
        Triple("Compiler Design Parsing", "Gate Smashers", "CD"),
        Triple("UML Diagrams Explained", "Tutorials Point", "SE"),
        Triple("Recurrence Relations", "Neso Academy", "Maths"),
        Triple("HTML CSS JS Revision", "CodeHelp", "Web"),
        Triple("Cache Mapping Techniques", "Education 4u", "COA"),
        Triple("Normalization in DBMS", "Jenny's Lectures", "DBMS"),
        Triple("Process Synchronization", "Knowledge Gate", "OS")
    ).mapIndexed { index, item ->
        VideoRecommendation(
            id = "video-$index",
            title = item.first,
            channelName = item.second,
            duration = "${8 + index % 18}:${(12 + index * 7) % 60}".padStart(5, '0'),
            subject = item.third
        )
    }

    val studyCollections = listOf(
        "Data Structures Crash Course",
        "DBMS Interview Prep",
        "Operating System Revision",
        "CN End Sem Pack",
        "Java Placement Kit",
        "Python Lab Companion",
        "Machine Learning Starter Pack",
        "Cloud Computing Essentials",
        "Cyber Security Foundation",
        "Compiler Design Last Minute",
        "Software Engineering Toolkit",
        "Discrete Maths Exam Pack",
        "Web Tech Full Stack Notes",
        "COA Diagrams and Numericals",
        "SQL Practice Marathon"
    ).mapIndexed { index, title ->
        StudyCollection(
            id = "collection-$index",
            title = title,
            notes = 8 + index,
            pyqs = 3 + index % 6,
            playlists = 1 + index % 4,
            cheatSheets = 2 + index % 5
        )
    }

    val subjectHubs = listOf(
        "#DBMS", "#OS", "#CN", "#DSA", "#Java", "#Python", "#AI", "#ML", "#Cloud", "#CyberSecurity"
    )

    val topContributors = listOf(
        Triple("Ankit Sharma", "AS", 42),
        Triple("Priya Das", "PD", 38),
        Triple("Rahul Verma", "RV", 35),
        Triple("Sneha Patel", "SP", 33),
        Triple("Karan Malhotra", "KM", 31),
        Triple("Neha Singh", "NS", 29),
        Triple("Arjun Reddy", "AR", 27),
        Triple("Mitali Roy", "MR", 25),
        Triple("Yash Gupta", "YG", 23),
        Triple("Riya Kapoor", "RK", 21)
    ).mapIndexed { index, item ->
        Contributor(
            id = "contributor-$index",
            name = item.first,
            initials = item.second,
            uploads = item.third,
            rating = 4.9 - (index % 5) * 0.1
        )
    }

    val revisionCards = listOf(
        "Deadlock Conditions" to listOf("Mutual Exclusion", "Hold and Wait", "No Preemption", "Circular Wait"),
        "ACID Properties" to listOf("Atomicity", "Consistency", "Isolation", "Durability"),
        "OSI Layers" to listOf("Physical", "Data Link", "Network", "Transport"),
        "OOP Pillars" to listOf("Encapsulation", "Inheritance", "Polymorphism", "Abstraction"),
        "Graph Traversal" to listOf("BFS uses queue", "DFS uses stack", "Track visited", "Analyze edges"),
        "Normalization" to listOf("1NF removes repeating groups", "2NF removes partial dependency", "3NF removes transitive dependency"),
        "Scheduling Metrics" to listOf("Waiting time", "Turnaround time", "Response time", "Throughput"),
        "TCP Flags" to listOf("SYN", "ACK", "FIN", "RST"),
        "ML Metrics" to listOf("Accuracy", "Precision", "Recall", "F1 Score"),
        "Cloud Models" to listOf("IaaS", "PaaS", "SaaS", "FaaS"),
        "Compiler Phases" to listOf("Lexical", "Syntax", "Semantic", "Code generation"),
        "SQL Joins" to listOf("Inner", "Left", "Right", "Full"),
        "Cache Mapping" to listOf("Direct", "Associative", "Set associative"),
        "Cyber Basics" to listOf("Confidentiality", "Integrity", "Availability"),
        "Big O Quick Check" to listOf("O(1)", "O(log n)", "O(n)", "O(n log n)")
    ).mapIndexed { index, item ->
        RevisionCard(
            id = "revision-$index",
            title = item.first,
            points = item.second
        )
    }

    val discoverItems = List(30) { index ->
        when (index % 4) {
            0 -> DiscoverFeedItem.Note(
                id = "discover-note-$index",
                title = trendingNotes[index % trendingNotes.size].title,
                subject = trendingNotes[index % trendingNotes.size].subject,
                downloads = 300 + index * 41
            )
            1 -> DiscoverFeedItem.Video(
                id = "discover-video-$index",
                title = videoRecommendations[index % videoRecommendations.size].title,
                channelName = videoRecommendations[index % videoRecommendations.size].channelName,
                duration = videoRecommendations[index % videoRecommendations.size].duration
            )
            2 -> DiscoverFeedItem.Collection(
                id = "discover-collection-$index",
                title = studyCollections[index % studyCollections.size].title,
                resourceCount = 18 + index
            )
            else -> DiscoverFeedItem.ContributorPost(
                id = "discover-contributor-$index",
                name = topContributors[index % topContributors.size].name,
                initials = topContributors[index % topContributors.size].initials,
                message = "shared a new ${trendingNotes[index % trendingNotes.size].subject} revision resource"
            )
        }
    }
}
