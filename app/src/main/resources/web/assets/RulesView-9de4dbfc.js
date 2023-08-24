import{_ as ce}from"./FieldId-e37ba71f.js";import{_ as re}from"./Breadcrumb-731a6814.js";import{d as ae,B as ue,r as N,u as le,Y as Q,ae as R,bT as C,bU as pe,ab as me,J as _e,ac as K,o as c,T as fe,x as T,b as e,f as n,M as f,bV as F,c as r,F as y,z as w,g as d,ag as O,Q as Z,N as x,e as A,k as B,ai as ve,i as ge,t as be,a1 as J,bW as he,bX as $e,P as ke,R as ee,S as te,w as oe,a0 as L}from"./index-66bea2e9.js";import{T as h,a as $,_ as ye,A as we}from"./question-mark-rounded-fdf2fb13.js";import{_ as Ce}from"./DeleteConfirm.vuevuetypescriptsetuptruelang-f7245844.js";import{_ as Ve}from"./VModal.vuevuetypescriptsetuptruelang-091ab08d.js";import{u as Te,a as Fe}from"./vee-validate.esm-18b68f3d.js";import"./stringToArray-243d75d5.js";const Ne={class:"row"},Ae={class:"col-md-3 col-form-label"},De={class:"col-md-9 form-checks"},Ie={class:"form-check form-check-inline"},Me={class:"form-check-label",for:"action-allow"},Ue={class:"form-check form-check-inline"},Se={class:"form-check-label",for:"action-block"},Ee={class:"row mb-2"},qe={for:"action",class:"col-md-3 col-form-label"},Re={class:"col-md-9 form-checks"},Oe={class:"form-check form-check-inline"},Be={class:"form-check-label",for:"direction-inbound"},Je={class:"form-check form-check-inline"},Le={class:"form-check-label",for:"direction-outbound"},Qe={class:"row mb-3"},je={class:"col-md-3 col-form-label"},ze={class:"col-md-9"},Ge=["value"],Pe={key:0,class:"input-group mt-2"},We=["placeholder"],Xe={class:"inner"},Ye={class:"help-block"},He={value:""},Ke=["value"],Ze={key:2,class:"invalid-feedback"},xe={class:"row mb-3"},et={class:"col-md-3 col-form-label"},tt={class:"col-md-9"},ot={value:"all"},nt=["value"],at=["value"],lt={class:"row mb-3"},st={class:"col-md-3 col-form-label"},it={class:"col-md-9"},dt=["disabled"],ne=ae({__name:"EditRuleModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(k){var j,z,G,P,W,X,Y;const v=k,{handleSubmit:b}=Te(),s=ue({action:"block",direction:"inbound",protocol:"all",apply_to:"all",notes:"",target:"",is_enabled:!0}),u=N(h.DNS),D=Object.values(h),{t:I}=le(),{mutate:M,loading:U,onDone:S}=Q({document:R`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${C}
  `,options:{update:(a,i)=>{pe(a,i.data.createConfig,R`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${C}
        `)}}}),{mutate:E,loading:q,onDone:l}=Q({document:R`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${C}
  `}),{value:p,resetField:m,errorMessage:g}=Fe("inputValue",me().test("required",a=>"valid.required",a=>!$.hasInput(u.value)||!!a).test("target-value",a=>"invalid_value",a=>$.isValid(u.value,a??""))),t=(j=v.data)==null?void 0:j.data;s.action=(t==null?void 0:t.action)??"block",s.direction=(t==null?void 0:t.direction)??"inbound",s.protocol=(t==null?void 0:t.protocol)??"all",u.value=((G=(z=v.data)==null?void 0:z.target)==null?void 0:G.type)??h.DNS,p.value=((W=(P=v.data)==null?void 0:P.target)==null?void 0:W.value)??"",s.apply_to=((Y=(X=v.data)==null?void 0:X.applyTo)==null?void 0:Y.toValue())??"all",s.notes=(t==null?void 0:t.notes)??"",s.is_enabled=(t==null?void 0:t.is_enabled)??!0,t||m(),_e(u,(a,i)=>{(a===h.INTERFACE||i===h.INTERFACE)&&(p.value="")});const _=b(()=>{const a=new $;a.type=u.value,a.value=p.value??"",s.target=a.toValue(),v.data?E({id:v.data.id,input:{group:"rule",value:JSON.stringify(s)}}):M({input:{group:"rule",value:JSON.stringify(s)}})});return S(()=>{K()}),l(()=>{K()}),(a,i)=>{const se=ye,ie=ve,de=Ve;return c(),fe(de,{title:d(t)?a.$t("edit"):a.$t("create")},{body:T(()=>{var V,H;return[e("div",Ne,[e("label",Ae,n(a.$t("actions")),1),e("div",De,[e("div",Ie,[f(e("input",{class:"form-check-input",type:"radio",name:"action",id:"action-allow",value:"allow","onUpdate:modelValue":i[0]||(i[0]=o=>s.action=o)},null,512),[[F,s.action]]),e("label",Me,n(a.$t("allow")),1)]),e("div",Ue,[f(e("input",{class:"form-check-input",type:"radio",name:"action",id:"action-block",value:"block","onUpdate:modelValue":i[1]||(i[1]=o=>s.action=o)},null,512),[[F,s.action]]),e("label",Se,n(a.$t("block")),1)])])]),e("div",Ee,[e("label",qe,n(a.$t("direction")),1),e("div",Re,[e("div",Oe,[f(e("input",{class:"form-check-input",type:"radio",name:"direction",id:"direction-inbound",value:"inbound","onUpdate:modelValue":i[2]||(i[2]=o=>s.direction=o)},null,512),[[F,s.direction]]),e("label",Be,n(a.$t("inbound")),1)]),e("div",Je,[f(e("input",{class:"form-check-input",type:"radio",name:"direction",id:"direction-outbound",value:"outbound","onUpdate:modelValue":i[3]||(i[3]=o=>s.direction=o)},null,512),[[F,s.direction]]),e("label",Le,n(a.$t("outbound")),1)])])]),e("div",Qe,[e("label",je,n(a.$t("match")),1),e("div",ze,[f(e("select",{class:"form-select","onUpdate:modelValue":i[4]||(i[4]=o=>u.value=o)},[(c(!0),r(y,null,w(d(D),o=>(c(),r("option",{value:o},n(a.$t(`target_type.${o}`)),9,Ge))),256))],512),[[O,u.value]]),d($).hasInput(u.value)?(c(),r("div",Pe,[f(e("input",{type:"text",class:"form-control","onUpdate:modelValue":i[5]||(i[5]=o=>Z(p)?p.value=o:null),placeholder:a.$t("for_example")+" "+d($).hint(u.value)},null,8,We),[[x,d(p)]]),A(ie,{class:"input-group-text"},{content:T(()=>[e("pre",Ye,n(a.$t(`examples_${u.value}`)),1)]),default:T(()=>[e("span",Xe,[A(se,{class:"bi"})])]),_:1})])):B("",!0),u.value===d(h).INTERFACE?f((c(),r("select",{key:1,class:"form-select mt-2","onUpdate:modelValue":i[6]||(i[6]=o=>Z(p)?p.value=o:null)},[e("option",He,n(a.$t("all_local_networks")),1),(c(!0),r(y,null,w((V=k.networks)==null?void 0:V.filter(o=>o.type!=="wan"),o=>(c(),r("option",{value:o.ifName},n(o.name),9,Ke))),256))],512)),[[O,d(p)]]):B("",!0),d(g)?(c(),r("div",Ze,n(d(g)?a.$t(d(g)):""),1)):B("",!0)])]),e("div",xe,[e("label",et,n(d(I)("apply_to")),1),e("div",tt,[f(e("select",{class:"form-select","onUpdate:modelValue":i[7]||(i[7]=o=>s.apply_to=o)},[e("option",ot,n(a.$t("all_devices")),1),(c(!0),r(y,null,w((H=k.networks)==null?void 0:H.filter(o=>o.type!=="wan"),o=>(c(),r("option",{key:o.ifName,value:"iface:"+o.ifName},n(o.name),9,nt))),128)),(c(!0),r(y,null,w(k.devices,o=>(c(),r("option",{value:"mac:"+o.mac},n(o.name),9,at))),256))],512),[[O,s.apply_to]])])]),e("div",lt,[e("label",st,n(a.$t("notes")),1),e("div",it,[f(e("textarea",{class:"form-control","onUpdate:modelValue":i[8]||(i[8]=o=>s.notes=o),rows:"3"},null,512),[[x,s.notes]])])])]}),action:T(()=>[e("button",{type:"button",disabled:d(U)||d(q),class:"btn",onClick:i[9]||(i[9]=(...V)=>d(_)&&d(_)(...V))},n(a.$t("save")),9,dt)]),_:1},8,["title"])}}}),ct={class:"page-container container"},rt={class:"main"},ut={class:"v-toolbar"},pt={class:"table"},mt=e("th",null,"ID",-1),_t={class:"actions two"},ft={class:"form-check"},vt=["disabled","onChange","onUpdate:modelValue"],gt=["title"],bt=["title"],ht={class:"actions two"},$t=["onClick"],kt=["onClick"],Dt=ae({__name:"RulesView",setup(k){const v=N([]),b=N([]),s=N([]),{t:u}=le();ge({handle:(l,p)=>{p?be(u(p),"error"):(v.value=l.configs.filter(m=>m.group==="rule").map(m=>{const g=JSON.parse(m.value),t=new we;t.parse(g.apply_to);const _=new $;return _.parse(g.target),{id:m.id,createdAt:m.createdAt,updatedAt:m.updatedAt,data:g,applyTo:t,target:_}}),b.value=[...l.devices],s.value=[...l.networks])},document:J`
    query {
      configs {
        ...ConfigFragment
      }
      devices {
        ...DeviceFragment
      }
      networks {
        ...NetworkFragment
      }
    }
    ${he}
    ${C}
    ${$e}
  `});const D=J`
  mutation DeleteConfig($id: ID!) {
    deleteConfig(id: $id)
  }
`;function I(l){L(Ce,{id:l.id,name:l.id,gql:D,appApi:!1,typeName:"Config"})}function M(l){L(ne,{data:l,devices:b,networks:s})}function U(){L(ne,{data:null,devices:b,networks:s})}const{mutate:S,loading:E}=Q({document:J`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${C}
  `});function q(l){S({id:l.id,input:{group:"rule",value:JSON.stringify(l.data)}})}return(l,p)=>{const m=re,g=ce;return c(),r("div",ct,[e("div",rt,[e("div",ut,[A(m,{current:()=>l.$t("page_title.rules")},null,8,["current"]),e("button",{type:"button",class:"btn right-actions",onClick:U},n(l.$t("create")),1)]),e("table",pt,[e("thead",null,[e("tr",null,[mt,e("th",null,n(l.$t("apply_to")),1),e("th",null,n(l.$t("description")),1),e("th",null,n(l.$t("notes")),1),e("th",null,n(l.$t("enabled")),1),e("th",null,n(l.$t("created_at")),1),e("th",null,n(l.$t("updated_at")),1),e("th",_t,n(l.$t("actions")),1)])]),e("tbody",null,[(c(!0),r(y,null,w(v.value,t=>(c(),r("tr",{key:t.id},[e("td",null,[A(g,{id:t.id,raw:t.data},null,8,["id","raw"])]),e("td",null,n(t.applyTo.getText(l.$t,b.value,s.value)),1),e("td",null,n(l.$t(`rule_${t.data.direction}`,{action:l.$t(t.data.action),target:t.target.getText(l.$t,s.value)})),1),e("td",null,n(t.data.notes),1),e("td",null,[e("div",ft,[f(e("input",{class:"form-check-input",disabled:d(E),onChange:_=>q(t),"onUpdate:modelValue":_=>t.data.is_enabled=_,type:"checkbox"},null,40,vt),[[ke,t.data.is_enabled]])])]),e("td",{class:"nowrap",title:d(ee)(t.createdAt)},n(d(te)(t.createdAt)),9,gt),e("td",{class:"nowrap",title:d(ee)(t.updatedAt)},n(d(te)(t.updatedAt)),9,bt),e("td",ht,[e("a",{href:"#",class:"v-link",onClick:oe(_=>M(t),["prevent"])},n(l.$t("edit")),9,$t),e("a",{href:"#",class:"v-link",onClick:oe(_=>I(t),["prevent"])},n(l.$t("delete")),9,kt)])]))),128))])])])])}}});export{Dt as default};
