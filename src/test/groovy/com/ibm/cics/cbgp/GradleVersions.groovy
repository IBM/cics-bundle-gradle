package com.ibm.cics.cbgp

abstract class GradleVersions {

    static List GRADLE_VERSIONS = ["8.5", "9.4.1"]

    static List onAllVersions(List inputs) {
        [GRADLE_VERSIONS, inputs]
            .combinations()
            .collect { i -> [i[0]] + i[1] }
    }

}