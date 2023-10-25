package com.ibm.cics.cbgp

abstract class GradleVersions {

    static List GRADLE_VERSIONS = ["7.6.1", "8.3"]

    static List onAllVersions(List inputs) {
        [GRADLE_VERSIONS, inputs]
            .combinations()
            .collect { i -> [i[0]] + i[1] }
    }

}