// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

// Child contract
contract Child {}

// Parent contract
contract Parent {
    Child public child;

    constructor() {
        // Deploy the Child contract
        child = new Child();
    }
}
