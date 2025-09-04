/*
 * Copyright 2022, by the California Institute of Technology.
 * ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology
 * Transfer at the California Institute of Technology.
 *
 * This software may be subject to U.S. export control laws. By accepting
 * this software, the user agrees to comply with all applicable U.S.
 * export laws and regulations. User has the responsibility to obtain
 * export licenses, or other export authority as may be required before
 * exporting such information to foreign countries or providing access to
 * foreign persons.
 */
/**
 * @author panjames
 */
import {useEffect, useState} from "react";

function getStorageValueJson(key, defaultValue) {
    const saved = localStorage.getItem(key)
    const initial = JSON.parse(saved)
    return initial || defaultValue
}

function getStorageValueBool(key, defaultValue) {
    const saved = localStorage.getItem(key)
    const value = (saved == null ? defaultValue : saved === 'true')
    return value
}

export const useLocalStorageJson = (key, defaultValue) => {
    const [value, setValue] = useState(() => {
        return getStorageValueJson(key, defaultValue)
    })

    useEffect(() => {
        localStorage.setItem(key, JSON.stringify(value))
    }, [key, value])

    return [value, setValue]
}

export const useLocalStorageBool = (key, defaultValue) => {
    const [value, setValue] = useState(() => {
        return getStorageValueBool(key, defaultValue)
    })

    useEffect(() => {
        localStorage.setItem(key, value)
    }, [key, value])

    return [value, setValue]
}