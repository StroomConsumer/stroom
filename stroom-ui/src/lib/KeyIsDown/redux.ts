import { Action, ActionCreator } from "redux";
import { prepareReducer } from "../redux-actions-ts";

export interface StoreState {
  [s: string]: boolean;
}

const defaultState = {};

export interface KeyChangeAction {
  keyCode: string;
  isDown: boolean;
}
export interface KeyUpAction extends KeyChangeAction, Action<"KEY_UP"> {}
export interface KeyDownAction extends KeyChangeAction, Action<"KEY_DOWN"> {}

const KEY_UP = "KEY_UP";
const KEY_DOWN = "KEY_DOWN";

export interface ActionCreators {
  keyDown: ActionCreator<KeyDownAction>;
  keyUp: ActionCreator<KeyUpAction>;
}

export const actionCreators: ActionCreators = {
  keyDown: (keyCode: string) => ({ type: KEY_DOWN, keyCode, isDown: true }),
  keyUp: (keyCode: string) => ({ type: KEY_UP, keyCode, isDown: false })
};

export const reducer = prepareReducer(defaultState)
  .handleActions<KeyChangeAction & Action>(
    [KEY_DOWN, KEY_UP],
    (state, { keyCode, isDown }) => ({
      ...state,
      [keyCode]: isDown
    })
  )
  .getReducer();