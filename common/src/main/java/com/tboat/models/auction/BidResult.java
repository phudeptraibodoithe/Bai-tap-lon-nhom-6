package com.tboat.models.auction;

public enum BidResult {
    OK,
    PRICE_TOO_LOW,
    INSUFFICIENT_BALANCE,
    SESSION_NOT_FOUND,
    USER_NOT_FOUND,
    DB_ERROR
}
