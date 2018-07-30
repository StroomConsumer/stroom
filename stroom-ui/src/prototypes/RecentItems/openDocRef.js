import { actionCreators } from './redux';

const { docRefOpened } = actionCreators;

export default (history, docRef) => (dispatch, getState) => {
  dispatch(docRefOpened(docRef));
  history.push(`/s/doc/${docRef.type}/${docRef.uuid}`);
};